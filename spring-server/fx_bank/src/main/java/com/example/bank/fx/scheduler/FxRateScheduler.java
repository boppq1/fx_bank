package com.example.bank.fx.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.bank.fx.dao.FxDataDao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FxRateScheduler {

    private static final Logger log = LoggerFactory.getLogger(FxRateScheduler.class);
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final RestTemplate restTemplate;
    private final FxDataDao fxDataDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${fx.exim.api-key}")
    private String authKey;

    @Value("${fx.exim.base-url}")
    private String baseUrl;

    @Scheduled(cron = "${fx.exim.cron}", zone = "Asia/Seoul")
    public void insertRate() {
        loadTodayRates("daily");
    }

    @Scheduled(cron = "${fx.exim.retry-cron:0 10 12-18 * * MON-FRI}", zone = "Asia/Seoul")
    public void retryInsertRate() {
        loadTodayRates("retry");
    }

    private void loadTodayRates(String trigger) {
        LocalDate today = LocalDate.now(SEOUL);
        String searchdate = today.format(YMD);

        int alreadyLoaded = fxDataDao.countRatesByDate(searchdate);
        if (alreadyLoaded > 0) {
            log.info("[FX] {} skip. exchange rates already loaded for {}. count={}", trigger, searchdate, alreadyLoaded);
            return;
        }

        boolean stored = fetchAndStore(searchdate);
        if (stored) {
            log.info("[FX] {} load success. searchdate={}", trigger, searchdate);
        } else {
            log.warn("[FX] {} load skipped or failed. searchdate={}", trigger, searchdate);
        }
    }

    private boolean fetchAndStore(String searchdate) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .queryParam("authkey", authKey)
                .queryParam("searchdate", searchdate)
                .queryParam("data", "AP01")
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isBlank()) {
                log.warn("[FX] empty response. searchdate={}", searchdate);
                return false;
            }

            String trimmed = response.trim();
            if (!trimmed.startsWith("[")) {
                log.warn("[FX] unexpected response. searchdate={}, response={}", searchdate, trimmed);
                return false;
            }

            List<Map<String, Object>> list = objectMapper.readValue(
                    trimmed,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            if (list == null || list.isEmpty()) {
                log.warn("[FX] no announced exchange rates yet. searchdate={}", searchdate);
                return false;
            }

            String announcedAt = searchdate + "120000";
            int inserted = 0;
            for (Map<String, Object> item : list) {
                String currencyCode = asText(item.get("cur_unit"));
                Double ttb = parseRate(item.get("ttb"));
                Double tts = parseRate(item.get("tts"));
                Double dealBasR = parseRate(item.get("deal_bas_r"));

                if (currencyCode == null || ttb == null || tts == null || dealBasR == null) {
                    continue;
                }

                fxDataDao.insertRate(currencyCode, ttb, tts, dealBasR, announcedAt);
                inserted++;
            }

            log.info("[FX] inserted exchange rates. searchdate={}, count={}", searchdate, inserted);
            return inserted > 0;
        } catch (Exception e) {
            log.error("[FX] failed to load exchange rates. searchdate={}", searchdate, e);
            return false;
        }
    }

    private String asText(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Double parseRate(Object value) {
        String text = asText(value);
        if (text == null || "-".equals(text)) return null;

        try {
            double parsed = Double.parseDouble(text.replace(",", ""));
            return parsed == 0 ? null : parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
