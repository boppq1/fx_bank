package com.example.bank.personal.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [사용 위치: 회원가입 / 로그인 만료체크 / OCR 재인증 / 스케줄러 청소]
 * user_sensitive_infos 테이블 매핑 전용 Entity (주민등록번호 전용).
 * users 테이블과 user_no(FK)로 연결된다.
 */
@Data
public class UserSensitiveInfoEntity {
    private Long sensitiveNo;            // SENSITIVE_NO (NUMBER, PK)
    private Long userNo;                 // USER_NO (NUMBER, FK -> users.user_no)
    private String rrnEnc;              // RRN_ENC (VARCHAR2, AES 암호화된 주민번호 원문)
    private String rrnMasked;          // RRN_MASKED (VARCHAR2, 마스킹 주민번호)
    private LocalDateTime retentionUntilDt; // RETENTION_UNTIL_DT (DATE, 보관 만료일 = 인증시점 + 3일)
}
