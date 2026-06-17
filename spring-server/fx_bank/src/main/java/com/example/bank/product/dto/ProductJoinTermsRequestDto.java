package com.example.bank.product.dto;
// 약관 공의 화면에서 체크한 값 받을 때 사용

import java.util.List;

import lombok.Data;

@Data
public class ProductJoinTermsRequestDto {
	private Long productNo;
	private List<String> requiredTermsCodes; // 사용자가 동의한 필수약관 코드 목록
	private List<String> optionalTermsCodes; // 사용자가 동의한 선택약관 코드 목록
}
