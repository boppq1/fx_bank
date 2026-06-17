package com.example.bank.personal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [사용 위치: 로그인/조회]
 * users 테이블 매핑 전용 Entity.
 * ⚠️ 주민등록번호(rrn)는 더 이상 users 테이블에 존재하지 않으며,
 *    user_sensitive_infos 테이블({@link UserSensitiveInfoEntity})로 분리되었다.
 */
@Data
public class UserEntity {
    private Long userNo;           // USER_NO (NUMBER, PK)
    private String userId;         // USER_ID (VARCHAR2)
    private String secuPw;         // SECU_PW (VARCHAR2, BCrypt 해시)
    private String nameKo;         // NAME_KO (VARCHAR2)
    private String nameEn;         // NAME_EN (VARCHAR2)
    private String phone;          // PHONE (VARCHAR2)
    private String email;          // EMAIL (VARCHAR2)
    private LocalDate birthDate;   // BIRTH_DATE (DATE)
    private String addrKo;         // ADDR_KO (VARCHAR2)
    private String addrDetailKo;   // ADDR_DETAIL_KO (VARCHAR2)
    private String zipCodeKo;      // ZIP_CODE_KO (VARCHAR2)
    private String addrEn;         // ADDR_EN (VARCHAR2)
    private String addrDetailEn;   // ADDR_DETAIL_EN (VARCHAR2)
    private String zipCodeEn;      // ZIP_CODE_EN (VARCHAR2)
    private String gender;         // GENDER (VARCHAR2)
    private String userTendency;   // USER_TENDENCY (VARCHAR2)
    private Integer sentimentScore; // SENTIMENT_SCORE (NUMBER)
    private LocalDateTime createdDt;   // CREATED_DT (DATE)
    private LocalDateTime updatedDt;   // UPDATED_DT (DATE)
    private LocalDateTime lastLoginAt; // LAST_LOGIN_AT (TIMESTAMP)
}
