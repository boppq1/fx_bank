package com.example.bank.personal.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RegisterRequestDto {
    private String userId;
    private String secuPw;
    private String nameKo;
    private String nameEn;
    private String rrn;
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
}