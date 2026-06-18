package com.example.bank.personal.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * [사용 위치: 회원가입]
 * 회원가입 폼에서 넘어오는 값. users + user_sensitive_infos 두 테이블에 나누어 INSERT 된다.
 * - rrn        : 사용자가 보정한 주민번호 원문(앞6 + 뒤7). AES 암호화되어 rrn_enc 에 저장된다. (★ users 에는 저장 안 함)
 * - rrnMasked  : OCR 이 내려준 마스킹 문자열(예: 030830-4******). rrn_masked 에 그대로 저장된다.
 * - privacyAgreed : 개인정보 수집·이용 동의 여부. false 면 가입 불가.
 * - userNo     : insertUser 시 <selectKey> 로 채번되어 채워지며, 이후 sensitive INSERT 에 재사용된다.
 */
@Getter
@Setter
@ToString
public class RegisterRequestDto {
    private Long userNo;        // <selectKey> 로 채번되는 users.user_no (sensitive INSERT 에 재사용)
    private String userId;
    private String secuPw;
    private String nameKo;
    private String nameEn;
    private String phone;
    private String email;
    private String addrKo;
    private String addrDetailKo;
    private String zipCodeKo;
    private String addrEn;
    private String addrDetailEn;
    private String zipCodeEn;
    private String gender;
    private String userTendency;
    private Date birthDate; // 주민번호 앞 7자리로 서버에서 계산하여 채움 (users.birth_date)

    // ===== 민감정보 (user_sensitive_infos 로 분리 저장) =====
    @ToString.Exclude
    private String rrn;         // 주민번호 원문(앞6+뒤7) → AES 암호화 후 rrn_enc 저장 (로그 노출 방지)
    private String rrnMasked;   // OCR 마스킹 문자열 → rrn_masked 저장

    // ===== 약관 동의 =====
    private boolean privacyAgreed; // 개인정보 수집·이용 동의(필수)
}
