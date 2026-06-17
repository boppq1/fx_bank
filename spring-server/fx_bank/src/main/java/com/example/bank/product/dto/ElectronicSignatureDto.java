package com.example.bank.product.dto;
// 전자 서명

import java.util.Date;

import lombok.Data;

@Data
public class ElectronicSignatureDto {
	private Long signatureNo;

	private Long subscriptionNo;
	private Long userNo;
	private Long verificationNo;

	private String signaturePath; // 손글씨 서명 이미지 저장 경로
	private String signedContent; // 사용자가 어떤 가입 내용에 서명 했는지 저장
	private Date signedDt; // 서명일
}
