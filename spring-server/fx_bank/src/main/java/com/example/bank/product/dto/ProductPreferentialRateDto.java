package com.example.bank.product.dto;

import java.math.BigDecimal;

import lombok.Data;

// 우대금리 DTO
@Data
public class ProductPreferentialRateDto {

    private Long preferentialRateNo; // 우대 금리 번호
    private Long productNo; // 어떤 상품의 우대 금리인지
    private String preferentialCondition; // 우대 조건
    private BigDecimal preferentialRate; // 우대율
}