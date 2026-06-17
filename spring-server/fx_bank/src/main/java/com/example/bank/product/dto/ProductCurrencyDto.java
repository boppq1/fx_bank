package com.example.bank.product.dto;

import lombok.Data;

// 상품별 가입 통화 DTO
@Data
public class ProductCurrencyDto {
	
	 private Long productCurrencyNo; // 상품 통화 번호
	 private Long productNo; // 어떤 상품의 통화인지 연결하는 값
	 private String currencyCode; // 통화 코드
	 private String currencyName; // 통화명
	
}
