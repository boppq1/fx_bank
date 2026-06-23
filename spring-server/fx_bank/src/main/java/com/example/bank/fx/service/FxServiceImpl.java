package com.example.bank.fx.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.bank.fx.dao.FxDataDao;
import com.example.bank.fx.dto.FxCalcResultDto;
import com.example.bank.fx.dto.FxDataDto;
import com.example.bank.fx.dto.FxRateCardDto;

import lombok.RequiredArgsConstructor;

/**
 * 환율 조회/계산 서비스.
 * 화면이 사용하는 환율은 전부 DB(exchange_rates)에서 읽는다. (외부 API 직접 호출 금지)
 * 외부 API 호출은 적재용 FxRateScheduler 에서만 수행한다.
 */
@Service
@RequiredArgsConstructor
public class FxServiceImpl implements FxService {

    private static final int SPARK_POINTS = 30;

    private final FxDataDao fxDataDao;

    /** 메인 페이지 노출 대표 통화 (application.properties: fx.main.currencies) */
    @Value("${fx.main.currencies:USD,JPY(100),CNH,EUR}")
    private String mainCurrencies;

    /** 메인 환율 카드/캐러셀 통화 세트 (대표 4종보다 넓게) */
    @Value("${fx.card.currencies:USD,JPY(100),CNH,EUR,GBP,AUD,CAD,CHF}")
    private String cardCurrencies;

    private List<String> mainCodes() {
        return splitCodes(mainCurrencies);
    }

    private List<String> cardCodes() {
        return splitCodes(cardCurrencies);
    }

    private List<String> splitCodes(String s) {
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public List<FxDataDto> getMainRates() {
        List<String> codes = mainCodes();
        if (codes.isEmpty()) {
            return Collections.emptyList();
        }
        return fxDataDao.selectLatestRatesByCurrencies(codes);
    }

    @Override
    public List<FxRateCardDto> getMainRateDetails() {
        List<FxRateCardDto> out = new ArrayList<>();
        for (String code : cardCodes()) {
            List<FxDataDto> hist = fxDataDao.selectRateHistory(code); // announced_at DESC
            if (hist == null || hist.isEmpty()) {
                continue;
            }

            FxDataDto latest = hist.get(0);
            FxDataDto prev = hist.size() > 1 ? hist.get(1) : latest;

            double base = nz(latest.getBaseRate());
            double prevBase = nz(prev.getBaseRate());
            double changeAbs = base - prevBase;
            double changePct = prevBase != 0 ? (changeAbs / prevBase) * 100.0 : 0.0;

            // 스파크라인: 최근 N개를 오래된→최신 순으로
            int n = Math.min(SPARK_POINTS, hist.size());
            List<Double> spark = new ArrayList<>();
            for (int i = n - 1; i >= 0; i--) {
                spark.add(round2(nz(hist.get(i).getBaseRate())));
            }

            out.add(new FxRateCardDto(
                    code,
                    round2(base),
                    round2(nz(latest.getBuyRate())),
                    round2(nz(latest.getSellRate())),
                    round2(changeAbs),
                    round2(changePct),
                    spark));
        }
        return out;
    }

    @Override
    public List<FxDataDto> getLatestRates() {
        return fxDataDao.selectAllLatestRates();
    }

    @Override
    public List<FxDataDto> getAllRates() {
        return fxDataDao.selectAll();
    }

    @Override
    public List<FxDataDto> getRateHistory(String currencyCode) {
        return fxDataDao.selectRateHistory(currencyCode);
    }

    @Override
    public FxCalcResultDto calculate(String currencyCode, String date, String buySell, int prefer) {

        if (date == null || date.isBlank()) {
            date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        FxDataDto rate = fxDataDao.selectLatestRate(currencyCode, date);
        if (rate == null) {
            throw new IllegalArgumentException("해당 통화의 환율 데이터가 없습니다: " + currencyCode);
        }

        boolean isBuy = "buy".equalsIgnoreCase(buySell);

        // 고시환율: 고객이 '살 때'=sellRate(tts) / '팔 때'=buyRate(ttb)
        double noticeRate = isBuy ? nz(rate.getSellRate()) : nz(rate.getBuyRate());
        double baseRate = nz(rate.getBaseRate());
        double spread = Math.abs(noticeRate - baseRate);
        double preferDiscount = spread * (prefer / 100.0);
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

    private double nz(Double v) {
        return v == null ? 0.0 : v;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
