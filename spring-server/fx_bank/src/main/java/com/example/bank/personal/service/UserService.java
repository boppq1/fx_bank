package com.example.bank.personal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.personal.dao.IUser;
import com.example.bank.personal.dto.LoginRequestDto;
import com.example.bank.personal.dto.RegisterRequestDto;
import com.example.bank.personal.dto.UserEntity;
import com.example.bank.personal.dto.UserSensitiveInfoEntity;
import com.example.bank.util.EncryptUtil;
import com.example.bank.util.JwtUtil;
import com.example.bank.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper; // 🚨 JSON 변환기 추가

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IUser iUser;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;
    private final EncryptUtil encryptUtil; // 🔐 주민번호 AES 양방향 암복호화
    private final ObjectMapper objectMapper; // 🚨 자바 객체 ➔ JSON 문자열 변환기 주입

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public boolean isUserIdDuplicated(String userId) {
        UserEntity user = iUser.findByUserId(userId.trim());
        return user != null;
    }

    /** 신분증 OCR 성공 시 발급하는 1회용 인증 토큰 TTL (30분) */
    private static final long OCR_TOKEN_TTL_MS = 30 * 60 * 1000L;

    /**
     * [OCR 인증 토큰 발급] 신분증 OCR 성공 시 1회용 토큰을 Redis 에 저장하고 반환한다.
     * 회원가입(registerUser)에서 이 토큰을 검증·소비하므로, OCR 없이는 가입이 불가능하다.
     */
    public String issueOcrToken() {
        String token = java.util.UUID.randomUUID().toString();
        redisUtil.setDataExpire("OCR_VERIFIED:" + token, "1", OCR_TOKEN_TTL_MS);
        return token;
    }

    /**
     * [회원가입] users + user_sensitive_infos 두 테이블을 하나의 트랜잭션으로 묶어 INSERT.
     * 둘 중 하나라도 실패하면 @Transactional 에 의해 전체 롤백된다.
     * 처리 순서: ① 약관/입력 검증 → ② 비밀번호 BCrypt 인코딩
     *           → ③ users INSERT(채번된 user_no 확보) → ④ 주민번호 AES 암호화
     *           → ⑤ user_sensitive_infos INSERT(retention_until_dt = 가입시각 + 3일)
     */
    @Transactional
    public void registerUser(RegisterRequestDto registerRequest) {
        // ① 약관 동의(필수) 서버 검증
        if (!registerRequest.isPrivacyAgreed()) {
            throw new IllegalArgumentException("개인정보 수집 및 이용 동의가 필요합니다.");
        }
        // ①-OCR 신분증 OCR 인증 필수: OCR 성공 시 발급된 1회용 토큰이 Redis 에 살아있어야 가입 가능
        String ocrToken = registerRequest.getOcrToken();
        if (ocrToken == null || ocrToken.trim().isEmpty()
                || redisUtil.getData("OCR_VERIFIED:" + ocrToken.trim()) == null) {
            throw new IllegalArgumentException("신분증 OCR 인증을 먼저 완료해 주세요.");
        }
        // ①-0 DB NOT NULL 필수값 검증 (비어 있으면 NPE/제약위반 대신 명확한 메시지)
        requireText(registerRequest.getUserId(), "아이디");
        requireText(registerRequest.getSecuPw(), "비밀번호");
        requireText(registerRequest.getNameKo(), "한글 성명");
        requireText(registerRequest.getPhone(), "휴대폰 번호");
        requireText(registerRequest.getEmail(), "이메일");
        requireText(registerRequest.getGender(), "성별");
        // ①-1 비밀번호 정책 검증 (영문 + 숫자 + 특수문자 포함, 8자 이상)
        validatePassword(registerRequest.getSecuPw());
        if (isUserIdDuplicated(registerRequest.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (registerRequest.getRrn() == null || registerRequest.getRrn().trim().isEmpty()) {
            throw new IllegalArgumentException("주민등록번호(신분증 인증)가 필요합니다.");
        }
        // 마스킹(*) 이 남아있으면 가려진 뒷자리를 보정하지 않은 것 → 가입 불가
        String rrn = registerRequest.getRrn().trim();
        String rrnDigits = rrn.replaceAll("[^0-9]", "");
        if (rrn.contains("*") || rrnDigits.length() != 13) {
            throw new IllegalArgumentException("주민등록번호 13자리(앞6 + 뒤7)를 정확히 입력해 주세요.");
        }
        registerRequest.setRrn(rrn);

        // ①-2 주민번호 앞 7자리로 생년월일 계산하여 채움
        registerRequest.setBirthDate(parseBirthDate(rrnDigits));

        // ② 비밀번호 BCrypt 인코딩
        String rawPassword = registerRequest.getSecuPw().trim();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        registerRequest.setSecuPw(encodedPassword);

        // ③ users INSERT (<selectKey> 로 registerRequest.userNo 가 채워진다)
        int result = iUser.insertUser(registerRequest);
        if (result != 1) {
            throw new RuntimeException("회원가입 처리 중 데이터베이스 오류가 발생했습니다.");
        }

        // ④ 주민번호 원문 AES 암호화 + ⑤ 민감정보 INSERT
        saveSensitiveInfo(registerRequest.getUserNo(),
                registerRequest.getRrn().trim(),
                registerRequest.getRrnMasked());

        // ⑥ 사용한 OCR 인증 토큰 소비(1회용) — 재사용 방지
        redisUtil.deleteData("OCR_VERIFIED:" + ocrToken.trim());
    }

    /**
     * 주민번호를 AES 암호화하여 user_sensitive_infos 에 INSERT (retention_until_dt = SYSDATE + 3, XML 에서 처리).
     * 회원가입과 OCR 재인증에서 공통으로 사용한다.
     */
    /**
     * 주민번호 13자리(YYMMDD + 성별/세기코드 + ...)에서 생년월일(LocalDate) 추출.
     * 7번째 자리(성별코드)로 출생 세기를 판별한다.
     *   1,2,5,6 → 1900년대 / 3,4,7,8 → 2000년대 / 9,0 → 1800년대
     */
    private Date parseBirthDate(String rrnDigits) {
        try {
            int yy = Integer.parseInt(rrnDigits.substring(0, 2));
            int mm = Integer.parseInt(rrnDigits.substring(2, 4));
            int dd = Integer.parseInt(rrnDigits.substring(4, 6));
            char genderCode = rrnDigits.charAt(6);

            int century;
            switch (genderCode) {
                case '1': case '2': case '5': case '6': century = 1900; break;
                case '3': case '4': case '7': case '8': century = 2000; break;
                case '9': case '0': century = 1800; break;
                default: throw new IllegalArgumentException("주민등록번호 형식이 올바르지 않습니다.");
            }
            LocalDate birth = LocalDate.of(century + yy, mm, dd);
            // Oracle DATE 바인딩 안정성을 위해 java.util.Date 로 변환 (LocalDate 직접 바인딩은 드라이버 호환 문제)
            return Date.from(birth.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("주민등록번호로 생년월일을 확인할 수 없습니다. 번호를 확인해 주세요.");
        } catch (Exception e) {
            throw new IllegalArgumentException("주민등록번호 형식이 올바르지 않습니다.");
        }
    }

    /**
     * 비밀번호 정책 검증: 영문/숫자/특수문자를 각각 1개 이상 포함, 8자 이상.
     * (프론트에서도 동일하게 검증하지만 서버에서 최종 보장)
     */
    private void validatePassword(String pw) {
        String p = (pw == null) ? "" : pw.trim();
        boolean hasLetter  = p.matches(".*[A-Za-z].*");
        boolean hasDigit   = p.matches(".*[0-9].*");
        boolean hasSpecial = p.matches(".*[^A-Za-z0-9].*");
        if (p.length() < 8 || !hasLetter || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 모두 포함하여 8자 이상이어야 합니다.");
        }
    }

    /** DB NOT NULL 필수 입력값 검증 (null/공백이면 어떤 항목인지 알려준다) */
    private void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수 입력 항목입니다.");
        }
    }

    private void saveSensitiveInfo(Long userNo, String rawRrn, String rrnMasked) {
        UserSensitiveInfoEntity sensitive = new UserSensitiveInfoEntity();
        sensitive.setUserNo(userNo);
        sensitive.setRrnEnc(encryptUtil.encrypt(rawRrn));
        sensitive.setRrnMasked(rrnMasked);
        int sensitiveResult = iUser.insertUserSensitiveInfo(sensitive);
        if (sensitiveResult != 1) {
            throw new RuntimeException("민감정보 저장 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * [성능 최적화 버전] DB 검증 + 토큰 발급 + Redis 이중 적재 (토큰 & 유저 정보 캐싱)
     */
    @Transactional
    public Map<String, String> login(LoginRequestDto loginRequest) {
        // 1. 오라클 DB 회원 검증
        UserEntity user = iUser.findByUserId(loginRequest.getUserId().trim());
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 회원 계정입니다.");
        }
        
        String cleanedRawPassword = loginRequest.getSecuPw().trim();
        String cleanedDbPassword = user.getSecuPw().trim();

        if (!passwordEncoder.matches(cleanedRawPassword, cleanedDbPassword)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 1-2. 마지막 로그인 시각 갱신
        iUser.updateLastLoginAt(user.getUserNo());

        // 2. JWT 토큰 세트 발행
        Map<String, String> tokenSet = jwtUtil.generateTokenSet(user.getUserId(), user.getNameKo());

        // 2-2. 민감정보(주민번호) 보관 만료 레이지 체크.
        //      로그인 자체는 주민번호 유무와 무관하게 성공하되, 만료/부재 시 OCR 재인증 필요 플래그를 내려보낸다.
        boolean reauthRequired = isReauthRequired(user.getUserNo());
        tokenSet.put("reauthRequired", String.valueOf(reauthRequired));

        // 3. Redis 장부 1호: Refresh Token 적재 (7일 만료)
        redisUtil.setDataExpire("RT:" + user.getUserId(), tokenSet.get("refreshToken"), refreshExpiration);
        System.out.println("[Redis 토큰 기록] Key ➔ RT:" + user.getUserId());

        // 4. 🚨 Redis 장부 2호: 유저 전체 정보 캐싱 (효율성 극대화)
        try {
            // 자바 객체(UserEntity)를 -> JSON 문자열("{"userId":"qwer", ...}")로 압축 변환
            String userJsonStr = objectMapper.writeValueAsString(user);
            
            // 토큰 유효기간과 동일하게 레디스 메모리에 박아둠
            redisUtil.setDataExpire("USER:" + user.getUserId(), userJsonStr, refreshExpiration);
            System.out.println("[Redis 유저 캐시 기록 완료] Key ➔ USER:" + user.getUserId());
            
        } catch (Exception e) {
            // 캐싱 처리가 실패하더라도 로그인 자체가 튕기지 않도록 예외 차단 (로그만 남김)
            System.err.println("🚨 [Redis 에러] 유저 정보 캐싱 실패: " + e.getMessage());
        }

        // 5. 완성된 토큰 반환
        return tokenSet;
    }
    
    /**
     * [조용한 재발급 (Silent Refresh)]
     * 쿠키의 리프레시 토큰을 검증하고, 새 토큰 세트를 반환합니다. (RTR 방식)
     */
    @Transactional
    public Map<String, String> refresh(String refreshToken) {
        // 1. 토큰에서 유저 아이디 추출 (★ JwtUtil에 만들어둔 메서드명에 맞게 수정해주세요)
        String userId = jwtUtil.getUserId(refreshToken);

        // 2. 레디스 금고 장부 확인 (해킹/탈취 방어)
        String redisToken = redisUtil.getData("RT:" + userId);
        if (redisToken == null || !redisToken.equals(refreshToken)) {
            throw new IllegalArgumentException("변조되거나 만료된 리프레시 토큰입니다. 다시 로그인해주세요.");
        }

        // 3. 레디스 유저 캐시에서 정보 꺼내기 (오라클 DB 조회 생략!)
        String userJsonStr = redisUtil.getData("USER:" + userId);
        if (userJsonStr == null) {
            throw new IllegalArgumentException("유저 정보가 만료되었습니다. 다시 로그인해주세요.");
        }

        try {
            // JSON 글자를 다시 자바 객체로 조립
            UserEntity user = objectMapper.readValue(userJsonStr, UserEntity.class);

            // 4. 새 토큰 세트 발급 (보안을 위해 Refresh Token도 새로 발급하여 교체)
            Map<String, String> newTokenSet = jwtUtil.generateTokenSet(user.getUserId(), user.getNameKo());

            // 5. 레디스 장부 기한 연장 (새 토큰 덮어쓰기)
            redisUtil.setDataExpire("RT:" + user.getUserId(), newTokenSet.get("refreshToken"), refreshExpiration);
            redisUtil.setDataExpire("USER:" + user.getUserId(), userJsonStr, refreshExpiration);

            return newTokenSet;
        } catch (Exception e) {
            throw new RuntimeException("토큰 재발급 처리 중 시스템 오류가 발생했습니다.");
        }
    }
    
    /**
     * [로그아웃 (블랙리스트 진화 버전)]
     * 1. 리프레시 토큰 파기
     * 2. 엑세스 토큰 블랙리스트 등재 (잔여 시간만큼)
     */
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        
        // 1. 기존 로직: 레디스 장부(RT, USER) 파기
        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            try {
                // 🚨 개발자님의 JwtUtil 메서드명(getUserId)에 완벽하게 맞춤!
                String userId = jwtUtil.getUserId(refreshToken);
                redisUtil.deleteData("RT:" + userId);
                redisUtil.deleteData("USER:" + userId);
                System.out.println("🗑️ [로그아웃] 레디스 데이터 파기 완료: " + userId);
            } catch (Exception e) {
                System.out.println("⚠️ 로그아웃 중 리프레시 토큰 해독 실패 (무시됨)");
            }
        }

        // 2. 신규 로직: Access Token 블랙리스트 처리
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            // "Bearer " 글자 7개를 떼어내고 순수 토큰만 추출
            String resolveToken = accessToken.substring(7);
            
            try {
                // 방금 JwtUtil에 추가한 최신 규격의 남은 유효시간 계산기 작동!
                long expiration = jwtUtil.getExpiration(resolveToken);
                
                // 남은 시간이 0보다 크면, 딱 그 시간만큼만 블랙리스트에 가둬둠
                if (expiration > 0) {
                    redisUtil.setDataExpire("BLACKLIST:" + resolveToken, "logout", expiration);
                    System.out.println("⛔ [블랙리스트 등재 완료] 남은 시간(ms): " + expiration);
                }
            } catch (Exception e) {
                System.out.println("⚠️ 이미 만료된 Access Token이라 블랙리스트에 넣지 않습니다.");
            }
        }
    }

    /**
     * [레이지 만료 체크] 사용자의 민감정보 보관 만료 여부를 판단한다.
     * - 행이 없으면(부재) → 재인증 필요(true)
     * - retention_until_dt < 현재시각(만료) → 해당 행을 즉시 삭제하고 재인증 필요(true)
     * - 유효하면 false
     * 이렇게 하면 스케줄러 실행 시각과 무관하게 사용자 체감 만료가 정확히 3일로 적용된다.
     */
    @Transactional
    public boolean isReauthRequired(Long userNo) {
        UserSensitiveInfoEntity sensitive = iUser.findSensitiveByUserNo(userNo);
        if (sensitive == null || sensitive.getRetentionUntilDt() == null) {
            return true;
        }
        if (sensitive.getRetentionUntilDt().isBefore(LocalDateTime.now())) {
            // 만료 데이터 즉시 파기
            iUser.deleteSensitiveByUserNo(userNo);
            return true;
        }
        return false;
    }

    /**
     * [OCR 재인증] 만료/삭제된 사용자가 신분증을 재촬영하여 주민번호를 다시 등록.
     * 기존 잔존 데이터를 정리한 뒤 새로 INSERT 하여 retention_until_dt 를 다시 3일 뒤로 갱신한다.
     */
    @Transactional
    public void reauthenticate(String userId, String rawRrn, String rrnMasked) {
        UserEntity user = iUser.findByUserId(userId.trim());
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 회원 계정입니다.");
        }
        if (rawRrn == null || rawRrn.trim().isEmpty()) {
            throw new IllegalArgumentException("주민등록번호(신분증 인증)가 필요합니다.");
        }
        String rrn = rawRrn.trim();
        if (rrn.contains("*") || rrn.replaceAll("[^0-9]", "").length() != 13) {
            throw new IllegalArgumentException("주민등록번호 13자리(앞6 + 뒤7)를 정확히 입력해 주세요.");
        }
        // 만료/잔존 데이터 정리 후 재삽입 (한 사용자당 1건 유지)
        iUser.deleteSensitiveByUserNo(user.getUserNo());
        saveSensitiveInfo(user.getUserNo(), rrn, rrnMasked);
    }
}