package com.example.bank.fx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.bank.fx.service.FxService;

import lombok.RequiredArgsConstructor;

/**
 * FX 화면(페이지) 컨트롤러.
 * 메인 환율영역은 외부 API가 아닌 DB(exchange_rates)에서 읽어 모델로 주입한다.
 */
@Controller
@RequiredArgsConstructor
public class FxMainController {

    private final FxService fxService;

    /** 메인(FX 홈) — 대표 통화 최신 환율을 DB에서 조회해 주입 */
    @GetMapping("/")
    public String fxHome(Model model) {
        model.addAttribute("mainRates", fxService.getMainRates());
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
