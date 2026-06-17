package com.example.bank.product.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;
// product_subscriptions에 최종 가입 정보를 넣을 때 사용
@Data
public class ProductSubscriptionInsertDto {
	private Long subscriptionNo;

	private Long productNo;
	private Long userNo;
	private Long rateNo;
	private String type;

	private String acntNo;
	private String currencyCode;
	private BigDecimal amount;
	private Integer periodMonth;

	private Long verificationNo;

	private String subscriptionStatus;
	private Date maturityDt;
	private BigDecimal appliedRate;
	private Date rateChangedDt;
}
