package com.example.bank.product.dto;

import java.util.Date;

import lombok.Data;
// optional_terms_agreements 테이블에 가입 시 동의한 '선택약관' 1건을 넣을 때 사용
@Data
public class OptionalTermsAgreementDto {
	private Long optionalAgreementNo;

	private Long subscriptionNo; // 어떤 가입건의 동의인지 (product_subscriptions)
	private String termsCode;    // 동의한 약관 코드 (product_terms)
	private String termsTitle;   // 약관 제목 (동의 시점 스냅샷 저장)
	private String agreedYn;     // 동의 여부 (Y/N)
	private Date agreedDt;       // 동의일
}
