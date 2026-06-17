package com.example.bank.product.dto;

import java.math.BigDecimal;

import lombok.Data;

//가입 금액, 기간, 통화 선택 화면에서 넘어오는 값
@Data
public class ProductJoinFormRequestDto {
	private Long productNo;
	private Long rateNo;
	private String currencyCode; // 통화 코드
	private BigDecimal amount;
	private Integer periodMonth;
	private BigDecimal appliedRate;
}
