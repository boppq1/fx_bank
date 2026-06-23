package com.example.bank.fx.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.bank.fx.dto.FxDataDto;

@Mapper
public interface FxDataDao {

    /**
     * 환율 1행 적재 (스케줄러 전용).
     * buy_rate=ttb(전신환 매수율), sell_rate=tts(전신환 매도율), base_rate=deal_bas_r(매매기준율)
     */
    void insertRate(
            @Param("currency_code") String currencyCode,
            @Param("buy_rate") Double buyRate,
            @Param("sell_rate") Double sellRate,
            @Param("base_rate") Double baseRate
    );

    /** 대표 통화들의 최신 환율 (메인 페이지) */
    List<FxDataDto> selectLatestRatesByCurrencies(@Param("currencyCodes") List<String> currencyCodes);

    /** 전체 통화별 최신 환율 (메인/요약) */
    List<FxDataDto> selectAllLatestRates();

    /** 전체 환율 이력 (환율조회 페이지 - 클라이언트 페이지네이션) */
    List<FxDataDto> selectAll();

    /** 특정 통화 환율 이력 (환율조회 상세/차트) */
    List<FxDataDto> selectRateHistory(@Param("currencyCode") String currencyCode);

    /** 기준일(date) 이전 최신 환율 1건 (환전계산기) */
    FxDataDto selectLatestRate(@Param("currencyCode") String currencyCode, @Param("date") String date);
}
