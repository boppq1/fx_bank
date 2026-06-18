package com.example.bank.product.dto;

import java.util.Date;

import lombok.Data;
// required_terms_agreements 테이블에 가입 시 동의한 '필수약관' 1건을 넣을 때 사용
@Data
public class RequiredTermsAgreementDto {
	private Long requiredAgreementNo;

	private Long subscriptionNo; // 어떤 가입건의 동의인지 (product_subscriptions)
	private String termsCode;    // 동의한 약관 코드 (product_terms)
	private String agreedYn;     // 동의 여부 (Y/N)
	private Date agreedAt;       // 동의 시각
}
