package com.example.bank.fx.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * exchange_rates 테이블 매핑 DTO (통화별 환율 1행)
 *
 * 컬럼 ↔ 필드 (application.properties: map-underscore-to-camel-case=true 로 자동 매핑)
 *  fx_rate_no    -> fxRateNo
 *  currency_code -> currencyCode  (통화 코드: USD, JPY(100), CNH, EUR ...)
 *  buy_rate      -> buyRate       (전신환 매수율 = 한국수출입은행 ttb / 고객이 외화를 '팔 때' 적용)
 *  sell_rate     -> sellRate      (전신환 매도율 = 한국수출입은행 tts / 고객이 외화를 '살 때' 적용)
 *  base_rate     -> baseRate      (매매기준율 = deal_bas_r)
 *  announced_at  -> announcedAt   (고시 시각)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FxDataDto {

    private Long fxRateNo;
    private String currencyCode;
    private Double buyRate;
    private Double sellRate;
    private Double baseRate;
    private LocalDateTime announcedAt;
}
