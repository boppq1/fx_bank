package com.example.bank.product.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;
// 가입 완료 화면에 보여줄 값
@Data
public class ProductJoinCompleteDto {
	private Long subscriptionNo;

	private String productName;
	private String userName;

	private String acntNo;
	private String currencyCode;
	private BigDecimal amount;
	private Integer periodMonth;
	private BigDecimal appliedRate;

	private Date subscribedDt;
	private Date maturityDt;

	private String subscriptionStatus;
}
