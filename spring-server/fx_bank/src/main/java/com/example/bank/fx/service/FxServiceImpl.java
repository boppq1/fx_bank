package com.example.bank.fx.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.bank.fx.dao.FxDataDao;
import com.example.bank.fx.dto.FxCalcResultDto;
import com.example.bank.fx.dto.FxDataDto;

import lombok.RequiredArgsConstructor;

/**
 * 환율 조회/계산 서비스.
 * 화면이 사용하는 환율은 전부 DB(exchange_rates)에서 읽는다. (외부 API 직접 호출 금지)
 * 외부 API 호출은 적재용 FxRateScheduler 에서만 수행한다.
 */
@Service
@RequiredArgsConstructor
public class FxServiceImpl implements FxService {

    private final FxDataDao fxDataDao;

    /** 메인 페이지 노출 대표 통화 (application.properties: fx.main.currencies) */
    @Value("${fx.main.currencies:USD,JPY(100),CNH,EUR}")
    private String mainCurrencies;

    @Override
    public List<FxDataDto> getMainRates() {
        List<String> codes = Arrays.stream(mainCurrencies.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (codes.isEmpty()) {
            return Collections.emptyList();
        }
        return fxDataDao.selectLatestRatesByCurrencies(codes);
    }

    @Override
    public List<FxDataDto> getLatestRates() {
        return fxDataDao.selectAllLatestRates();
    }

    @Override
    public List<FxDataDto> getRateHistory(String currencyCode) {
        return fxDataDao.selectRateHistory(currencyCode);
    }

    @Override
    public FxCalcResultDto calculate(String currencyCode, String date, String buySell, int prefer) {

        // 기준일 미지정 시 오늘 날짜 사용
        if (date == null || date.isBlank()) {
            date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        FxDataDto rate = fxDataDao.selectLatestRate(currencyCode, date);
        if (rate == null) {
            throw new IllegalArgumentException("해당 통화의 환율 데이터가 없습니다: " + currencyCode);
        }

        boolean isBuy = "buy".equalsIgnoreCase(buySell);

        // 고시환율: 고객이 '살 때'=sellRate(tts) / '팔 때'=buyRate(ttb)
        double noticeRate = isBuy ? rate.getSellRate() : rate.getBuyRate();
        double baseRate   = rate.getBaseRate();

        // 스프레드 = |고시환율 - 매매기준율|
        double spread = Math.abs(noticeRate - baseRate);

        // 우대 절감액 = 스프레드 × 우대율(%)
        double preferDiscount = spread * (prefer / 100.0);

        // 적용환율: 살 때는 우대만큼 싸게(-), 팔 때는 비싸게(+)
        double appliedRate = isBuy ? noticeRate - preferDiscount : noticeRate + preferDiscount;

        FxCalcResultDto result = new FxCalcResultDto();
        result.setCurrencyCode(currencyCode);
        result.setBaseDate(rate.getAnnouncedAt() != null ? rate.getAnnouncedAt().toString() : date);
        result.setBaseRate(baseRate);
        result.setNoticeRate(noticeRate);
        result.setSpread(round2(spread));
        result.setPreferDiscount(round2(preferDiscount));
        result.setAppliedRate(round2(appliedRate));
        return result;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
