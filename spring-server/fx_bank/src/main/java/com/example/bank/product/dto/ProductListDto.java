package com.example.bank.product.dto;

import java.math.BigDecimal;

import lombok.Data;

// 상품 목록 화면용 DTO
@Data
public class ProductListDto {
	
    private Long productNo; // 상품 번호
    private String productName; // 상품명
    private String productType; // 상품 유형

    private BigDecimal baseRate; // 기본 금리
    private BigDecimal maxRate; // 최고 금리

    private Integer minPeriodMonth; // 최소 가입 기간
    private Integer maxPeriodMonth; // 최대 가입 기간

    private BigDecimal minAmount; // 최소 가입 금액
    private BigDecimal maxAmount; // 최대 가입 금액

    private String targetLarge; // 가입 대상(대분류)
    private String targetDetail; // 가입대상 (소분류)
    private String description; // 상품 짧은 설명
    private String productTendency; // 상품 성향

    private String currencyCodes; // 상품 통화

    private BigDecimal avgRating; // 리뷰 평균 점수 
    private Integer reviewCount; // 리뷰 개수
	
}
