package com.example.bank.fx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 환전 계산 결과 DTO (환전계산기 응답)
 * 1차 banking_system 의 ReturnCalculatorDto 를 2차 컨벤션으로 이식.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FxCalcResultDto {

    private String currencyCode;   // 통화 코드
    private String baseDate;       // 적용 환율 고시일시
    private Double baseRate;        // 매매기준율
    private Double noticeRate;      // 고시환율 (살 때=sellRate / 팔 때=buyRate)
    private Double spread;          // 스프레드 = |고시환율 - 매매기준율|
    private Double preferDiscount;  // 우대 절감액 = 스프레드 × 우대율(%)
    private Double appliedRate;     // 최종 적용환율
}
