package com.example.bank.product.dto;
// 전자서명 후 최종 가입 신청할 때 넘어오는 값.

import lombok.Data;

@Data
public class ProductJoinSubmitRequestDto {
	private Long productNo;
	private String signatureImageData; // 서명 이미지
}
