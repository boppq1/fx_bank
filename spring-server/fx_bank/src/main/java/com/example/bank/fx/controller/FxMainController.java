package com.example.bank.fx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * FX 화면(페이지) 컨트롤러.
 * 메인 환율영역은 DB(exchange_rates) 기반으로 /api/fx/** 를 통해 비동기 렌더한다.
 * (외부 API 직접 호출 없음)
 */
@Controller
public class FxMainController {

    /** 메인(FX 홈) */
    @GetMapping("/")
    public String fxHome() {
        return "fx-home";
    }

    /** 환율조회 페이지 */
    @GetMapping("/fx/exchange-rate")
    public String exchangeRate() {
        return "fx/exchange-rate";
    }

    /** 환전계산기 페이지 */
    @GetMapping("/fx/exchange-calculator")
    public String exchangeCalculator() {
        return "fx/exchange-calculator";
    }

    /** 외환 상품·서비스 안내 (단일 페이지 + 탭) */
    @GetMapping("/fx/guide")
    public String guide() {
        return "fx/guide";
    }
}
