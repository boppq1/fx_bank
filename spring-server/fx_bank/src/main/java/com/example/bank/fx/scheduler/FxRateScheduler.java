package com.example.bank.fx.scheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.bank.fx.dao.FxDataDao;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * 한국수출입은행 환율 OpenAPI(AP01) 일일 적재 스케줄러.
 *
 * 외부 API 호출은 '적재' 용도이며, 화면(메인/환율조회/계산기)은 DB(exchange_rates)만 조회한다.
 * 매핑: cur_unit -> currency_code, ttb -> buy_rate, tts -> sell_rate, deal_bas_r -> base_rate
 *
 * 00:10 같은 새벽 시각에는 당일 환율이 아직 고시되지 않아 빈 응답이 오므로,
 * 데이터가 있는 가장 최근 영업일까지 최대 7일 거슬러 조회하여 1일치를 적재한다.
 * (@EnableScheduling 은 com.example.bank.analysis.SchedulerConfig 에 선언되어 있음)
 */
@Component
@RequiredArgsConstructor
public class FxRateScheduler {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_LOOKBACK_DAYS = 7;

    private final RestTemplate restTemplate;          // AppConfig 의 @Bean 재사용
    private final FxDataDao fxDataDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${fx.exim.api-key}")
    private String authKey;

    @Value("${fx.exim.base-url}")
    private String baseUrl;

    @Scheduled(cron = "${fx.exim.cron}")
    public void insertRate() {
        LocalDate base = LocalDate.now();
        for (int offset = 0; offset <= MAX_LOOKBACK_DAYS; offset++) {
            String searchdate = base.minusDays(offset).format(YMD);
            if (fetchAndStore(searchdate)) {
                return; // 데이터가 있는 가장 최근 영업일 1건만 적재하고 종료
            }
        }
    }

    /** 해당 일자의 환율을 조회해 적재. 실제 적재가 1건 이상이면 true. */
    private boolean fetchAndStore(String searchdate) {
        String url = baseUrl + "?authkey=" + authKey + "&searchdate=" + searchdate + "&data=AP01";
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isBlank()) {
                return false; // 영업일/시간 외 빈 응답
            }

            List<Map<String, Object>> list = objectMapper.readValue(response, List.class);
            if (list == null || list.isEmpty()) {
                return false;
            }

            int inserted = 0;
            for (Map<String, Object> item : list) {
                Object ttbObj = item.get("ttb");
                if (ttbObj == null) continue;

                double ttb = Double.parseDouble(ttbObj.toString().replace(",", ""));
                if (ttb == 0) continue; // 유효하지 않은 통화 행 스킵

                // cur_unit = 통화 코드 (USD, JPY(100), CNH ...) -> currency_code 컬럼
                String currencyCode = (String) item.get("cur_unit");
                double tts      = Double.parseDouble(item.get("tts").toString().replace(",", ""));
                double dealBasR = Double.parseDouble(item.get("deal_bas_r").toString().replace(",", ""));

                fxDataDao.insertRate(currencyCode, ttb, tts, dealBasR);
                inserted++;
            }
            return inserted > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
