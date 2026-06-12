package com.example.bank.personal.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String userId;
    private String secuPw;
}
