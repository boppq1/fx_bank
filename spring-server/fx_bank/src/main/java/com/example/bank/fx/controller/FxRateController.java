package com.example.bank.fx.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bank.fx.dto.FxCalcResultDto;
import com.example.bank.fx.dto.FxDataDto;
import com.example.bank.fx.service.FxService;
import com.example.bank.gloval.common.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 환율 조회 / 환전 계산 REST API.
 * 모든 데이터는 DB(exchange_rates)에서 조회한다.
 */
@RestController
@RequestMapping("/api/fx")
@RequiredArgsConstructor
public class FxRateController {

    private final FxService fxService;

    /** 메인 페이지 대표 통화 최신 환율 */
    @GetMapping("/rates/main")
    public ApiResponse<List<FxDataDto>> mainRates() {
        return ApiResponse.success("메인 환율 조회 성공", fxService.getMainRates());
    }

    /** 전체 통화 최신 환율 (환율조회 표) */
    @GetMapping("/rates/latest")
    public ApiResponse<List<FxDataDto>> latestRates() {
        return ApiResponse.success("최신 환율 조회 성공", fxService.getLatestRates());
    }

    /** 특정 통화 환율 이력 (환율조회 상세) */
    @GetMapping("/rates")
    public ApiResponse<List<FxDataDto>> rateHistory(@RequestParam("currencyCode") String currencyCode) {
        return ApiResponse.success("환율 이력 조회 성공", fxService.getRateHistory(currencyCode));
    }

    /**
     * 환전 계산기.
     * @param currencyCode 통화 코드 (USD, JPY(100), CNH ...)
     * @param date         기준일 (yyyy-MM-dd, 미지정 시 오늘)
     * @param buySell      buy=고객이 외화 살 때 / sell=고객이 외화 팔 때
     * @param prefer       환율 우대율 (0~100, %)
     */
    @GetMapping("/calc")
    public ApiResponse<FxCalcResultDto> calc(
            @RequestParam("currencyCode") String currencyCode,
            @RequestParam(name = "date", required = false) String date,
            @RequestParam(name = "buySell", defaultValue = "buy") String buySell,
            @RequestParam(name = "prefer", defaultValue = "0") int prefer
    ) {
        try {
            return ApiResponse.success("환전 계산 성공", fxService.calculate(currencyCode, date, buySell, prefer));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
