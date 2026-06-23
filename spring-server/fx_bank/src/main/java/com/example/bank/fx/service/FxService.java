package com.example.bank.fx.service;

import java.util.List;

import com.example.bank.fx.dto.FxCalcResultDto;
import com.example.bank.fx.dto.FxDataDto;
import com.example.bank.fx.dto.FxRateCardDto;

public interface FxService {

    /** 메인 페이지 대표 통화 최신 환율 */
    List<FxDataDto> getMainRates();

    /** 메인 페이지 환율 카드 상세(전일대비 + 스파크라인) */
    List<FxRateCardDto> getMainRateDetails();

    /** 전체 통화 최신 환율 (요약) */
    List<FxDataDto> getLatestRates();

    /** 전체 환율 이력 (환율조회 페이지) */
    List<FxDataDto> getAllRates();

    /** 특정 통화 환율 이력 */
    List<FxDataDto> getRateHistory(String currencyCode);

    /** 환전 계산 (우대율 반영) */
    FxCalcResultDto calculate(String currencyCode, String date, String buySell, int prefer);
}
