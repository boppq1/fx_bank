package com.example.bank.personal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.personal.dao.IUser;
import com.example.bank.personal.dto.LoginRequestDto;
import com.example.bank.personal.dto.RegisterRequestDto;
import com.example.bank.personal.dto.UserEntity;
import com.example.bank.util.JwtUtil;
import com.example.bank.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper; // 🚨 JSON 변환기 추가

import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IUser iUser;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper; // 🚨 자바 객체 ➔ JSON 문자열 변환기 주입

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public boolean isUserIdDuplicated(String userId) {
        UserEntity user = iUser.findByUserId(userId.trim());
        return user != null;
    }

    @Transactional
    public void registerUser(RegisterRequestDto registerRequest) {
        if (isUserIdDuplicated(registerRequest.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        String rawPassword = registerRequest.getSecuPw().trim();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        registerRequest.setSecuPw(encodedPassword);

        int result = iUser.insertUser(registerRequest);
        if (result != 1) {
            throw new RuntimeException("회원가입 처리 중 데이터베이스 오류가 발생했습니다.");
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

        // 2. JWT 토큰 세트 발행
        Map<String, String> tokenSet = jwtUtil.generateTokenSet(user.getUserId(), user.getNameKo());

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
}