package com.example.bank.product.dto;

import java.math.BigDecimal;

import lombok.Data;

// 기간별 금리 DTO
@Data
public class ProductRateDto {

    private Long rateNo; // 금리 번호
    private Long productNo; // 어떤 상품의 금리 인지
    private Integer periodMonth; // 가입 기간
    private BigDecimal interestRate; // 해당 기간의 금리
}