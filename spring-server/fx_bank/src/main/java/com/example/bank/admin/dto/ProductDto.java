package com.example.bank.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 상품 (products 테이블)
 *
 * CREATE TABLE products (
 *     product_no NUMBER PRIMARY KEY,
 *     active CHAR(1) NOT NULL,
 *     product_name VARCHAR2(200) NOT NULL,
 *     product_type VARCHAR2(30) NOT NULL,
 *     base_rate NUMBER(5,2),
 *     max_rate NUMBER(5,2),
 *     min_period_month NUMBER,
 *     max_period_month NUMBER,
 *     min_amount NUMBER(18,2),
 *     max_amount NUMBER(18,2),
 *     target_large VARCHAR2(30),
 *     target_detail VARCHAR2(50),
 *     description CLOB,
 *     interest_payment_type VARCHAR2(50),
 *     product_tendency VARCHAR2(50),
 *     created_dt DATE DEFAULT SYSDATE NOT NULL,
 *     created_by NUMBER,
 *     approved_by NUMBER,
 *     updated_dt DATE,
 *     updated_by NUMBER,
 *     status VARCHAR2(30) NOT NULL,
 *     product_start_dt DATE,
 *     product_end_dt DATE
 * );
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long productNo;
    private String active;               // Y/N

    private String productName;
    private String productType;

    private BigDecimal baseRate;
    private BigDecimal maxRate;

    private Integer minPeriodMonth;
    private Integer maxPeriodMonth;

    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    private String targetLarge;
    private String targetDetail;

    private String description;          // CLOB

    private String interestPaymentType;
    private String productTendency;

    private String createdDt;
    private Long createdBy;
    private String createdByName;   // admins 조인 (작성자명)

    private Long approvedBy;
    private String approvedByName;  // admins 조인 (승인자명)

    private String updatedDt;
    private Long updatedBy;

    private String status;

    private String productStartDt;
    private String productEndDt;

    // 약관팀 목록 화면 전용 집계 필드 (selectProductListForTerms 조회시에만 채워짐)
    private Integer termsCount;     // 이 상품에 연결된 약관 슬롯 수
    private Integer pendingCount;   // (구) 승인요청 대기 건수 - 새 스키마 전환 후 의미 재정의 필요
}