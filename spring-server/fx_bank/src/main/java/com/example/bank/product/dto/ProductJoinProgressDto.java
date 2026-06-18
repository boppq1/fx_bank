package com.example.bank.product.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;
// product_join_progress 테이블에 가입 진행 중간상태를 저장/이어가기 할 때 사용
// (폼 → 약관 → 본인인증 → 서명 단계별로 갱신하고, 완료 시 실제 가입 테이블로 옮김)
@Data
public class ProductJoinProgressDto {
	private Long joinProgressNo;

	private Long userNo;
	private Long productNo;
	private Long verificationNo; // 본인인증 결과 연결 (id_verifications)

	private String currentStep;     // 현재 진행 단계 (예: FORM, TERMS, VERIFY, SIGN)
	private String progressStatus;  // 진행 상태 (기본 IN_PROGRESS)

	private String requiredTermsCodes; // 동의한 필수약관 코드 목록 (CLOB, 직렬화 문자열)
	private String optionalTermsCodes; // 동의한 선택약관 코드 목록 (CLOB, 직렬화 문자열)

	private Long rateNo;            // 선택한 금리
	private String currencyCode;    // 선택한 통화
	private BigDecimal amount;      // 가입 금액
	private Integer periodMonth;    // 가입 기간(개월)
	private BigDecimal appliedRate; // 적용 금리

	private Date createdDt;  // 진행 시작일
	private Date updatedDt;  // 마지막 갱신일
	private Date expiredDt;  // 진행 만료일 (방치 시 만료 처리용)
}
