package com.example.bank.fx.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메인 페이지 환율 카드용 DTO.
 * 최신 환율 + 전일대비(매매기준율) + 최근 추세(스파크라인용).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FxRateCardDto {

    private String currencyCode;
    private Double baseRate;       // 매매기준율(최신)
    private Double buyRate;        // ttb (고객이 팔 때)
    private Double sellRate;       // tts (고객이 살 때)
    private Double changeAbs;      // 전일대비 절대값(매매기준율)
    private Double changePct;      // 전일대비 %
    private List<Double> spark;    // 최근 매매기준율 추세(오래된→최신)
}
