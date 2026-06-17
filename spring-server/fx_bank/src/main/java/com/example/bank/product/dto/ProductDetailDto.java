package com.example.bank.product.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

// 상품 상세 화면 상단 기본 정보용 DTO
@Data
public class ProductDetailDto {

    private Long productNo; // 상품 번호
    private String active; // 판매 여부
    private String productName; // 상품명
    private String productType; // 상품 유형

    private BigDecimal baseRate; // 기본 금리
    private BigDecimal maxRate; // 최고 금리

    private Integer minPeriodMonth; // 가입가능 기간(최소)
    private Integer maxPeriodMonth; // 가입가능 기간 (최대)

    private BigDecimal minAmount; // 가입 가능 금액 (최소)
    private BigDecimal maxAmount; // 가입 가능 금액(최대)

    private String targetLarge; // 가입 대상 (대분류)
    private String targetDetail;// 가입대상 (소분류)
    private String description; // 상품 설명

    private String interestPaymentType; // 이자 지급 방식
    private String productTendency; // 상품 성향

    private String status; // 상품 상태
    private Date productStartDt; // 상품 판매 시작일
    private Date productEndDt; // 상품 판매 종료일

    private BigDecimal avgRating; // 리뷰 요약
    private Integer reviewCount; //리뷰 요약
}