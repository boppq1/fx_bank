package com.example.bank.product.dto;

import java.util.Date;

import lombok.Data;
// id_verifications 테이블에 넣을 DTO.
@Data
public class IdVerificationDto {
	private Long verificationNo;
	private Long userNo;
	private Long productNo;

	private String verificationStatus;
	private String verificationMethod;
	private String ocrProvider;

	private String matchedNameYn;
	private String matchedBirthYn;
	private String failReason;

	private Date verifiedDt;
	private Date expiredDt;
	private Date createdDt;	
}
