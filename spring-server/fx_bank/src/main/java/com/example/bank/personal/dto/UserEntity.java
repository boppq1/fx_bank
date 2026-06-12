package com.example.bank.personal.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserEntity {
	private Long userNo;          // USER_NO (NUMBER, PK)
    private String userId;        // USER_ID (VARCHAR2)
    private String secuPw;        // SECU_PW (VARCHAR2, 현재는 평문 '1234' 가정)
    private String nameKo;        // NAME_KO (VARCHAR2)
    private String nameEn;        // NAME_EN (VARCHAR2)
    private String rrn;           // RRN (VARCHAR2)
    private String phone;         // PHONE (VARCHAR2)
    private String email;         // EMAIL (VARCHAR2)
    private String addrKo;        // ADDR_KO (VARCHAR2)
    private String addrDetailKo;  // ADDR_DETAIL_KO (VARCHAR2)
    private String zipCodeKo;     // ZIP_CODE_KO (VARCHAR2)
    private String addrEn;        // ADDR_EN (VARCHAR2)
    private String addrDetailEn;  // ADDR_DETAIL_EN (VARCHAR2)
    private String zipCodeEn;     // ZIP_CODE_EN (VARCHAR2)
    private String gender;        // GENDER (VARCHAR2)
    private String userTendency;  // USER_TENDENCY (VARCHAR2)
    private LocalDateTime createdDt;   // CREATED_DT (DATE)
    private LocalDateTime updatedDt;   // UPDATED_DT (DATE)
    private LocalDateTime lastLoginAt; // LAST_LOGIN_AT (TIMESTAMP)
}
