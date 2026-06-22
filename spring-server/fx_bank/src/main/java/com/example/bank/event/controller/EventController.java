package com.example.bank.event.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.bank.event.dto.EventDto;
import com.example.bank.event.service.EventService;
import com.example.bank.util.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final JwtUtil jwtUtil;

    // 이벤트 설명 페이지
    @GetMapping
    public String eventPage(HttpServletRequest request, Model model) {
        String userAgent = request.getHeader("User-Agent");
        boolean isApp = userAgent != null && userAgent.contains("Flutter/fx_bank");
        model.addAttribute("isApp", isApp);
        return "event/event";
    }

    // 이벤트 현황 페이지
    @GetMapping("/status")
    public String eventStatus(HttpServletRequest request, Model model) {
        String token = extractToken(request);
        Long userNo = Long.parseLong(jwtUtil.getUserId(token));
        // 쿠폰 번호 고정값 설정 (필요 시 관리)
        Long couponNo = 1L; 

        EventDto event = eventService.getEvent(userNo);
        model.addAttribute("event", event);
        return "event/event-status";
    }

    // 이미지 업로드 & 추론 API (productNo -> couponNo 변경)
    @PostMapping("/detect")
    @ResponseBody
    public ResponseEntity<?> detect(
            HttpServletRequest request,
            @RequestParam Long couponNo,
            @RequestParam MultipartFile file) {
        String token = extractToken(request);
        Long userNo = Long.parseLong(jwtUtil.getUserId(token));
        EventDto result = eventService.uploadAndDetect(userNo, couponNo, file);
        return ResponseEntity.ok(result);
    }

    // 이벤트 참여 신청 API (productNo -> couponNo 변경)
    @PostMapping("/join")
    @ResponseBody
    public ResponseEntity<?> joinEvent(HttpServletRequest request,
                                       @RequestParam Long couponNo) {
        String token = extractToken(request);
        Long userNo = Long.parseLong(jwtUtil.getUserId(token));
        eventService.joinEvent(userNo, couponNo);
        return ResponseEntity.ok("이벤트 참여 완료");
    }

    // 쿠키에서 토큰 추출
    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new RuntimeException("토큰이 없습니다.");
    }
}