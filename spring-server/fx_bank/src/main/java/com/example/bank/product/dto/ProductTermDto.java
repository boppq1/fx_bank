package com.example.bank.product.dto;

import lombok.Data;

// 상품약관 DTO
@Data
public class ProductTermDto {

    private String termsCode; // 약관 코드
    private Long productNo; // 어떤 상품의 약관인지
    private String termsTitle; // 약관 제목
    private String termsType; // 약관 구분
    private String requiredYn; // 필수 여부
    private String pdfPath; // pdf 경로
    private String termsText; // 약관 텍스트
    private String useYn; // 사용 여부
}