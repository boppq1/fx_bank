package com.example.bank.event.controller;

import org.springframework.http.HttpStatus;
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
import com.example.bank.personal.dao.IUser;
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
    private final IUser iUser;

    @GetMapping
    public String eventPage(HttpServletRequest request, Model model) {
        if (!hasRefreshToken(request)) {
            return "redirect:/login?returnUrl=/event";
        }

        String userAgent = request.getHeader("User-Agent");
        boolean isApp = userAgent != null && userAgent.contains("Flutter/fx_bank");
        model.addAttribute("isApp", isApp);
        return "event/event";
    }

    @GetMapping("/status")
    public String eventStatus(HttpServletRequest request, Model model) {
        Long userNo = extractUserNoOrNull(request);
        if (userNo == null) {
            return "redirect:/login?returnUrl=/event/status";
        }

        try {
            EventDto event = eventService.getEvent(userNo);
            model.addAttribute("event", event);
            return "event/event-status";
        } catch (RuntimeException e) {
            return "redirect:/event";
        }
    }

    @PostMapping("/detect")
    @ResponseBody
    public ResponseEntity<?> detect(
            HttpServletRequest request,
            @RequestParam String letter,
            @RequestParam MultipartFile file) {
        Long userNo = extractUserNoOrNull(request);
        if (userNo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            EventDto result = eventService.uploadAndDetect(userNo, letter, file);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/join")
    @ResponseBody
    public ResponseEntity<?> joinEvent(HttpServletRequest request) {
        Long userNo = extractUserNoOrNull(request);
        if (userNo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        eventService.joinEvent(userNo);
        return ResponseEntity.ok("이벤트 참여 완료");
    }

    private boolean hasRefreshToken(HttpServletRequest request) {
        return extractToken(request) != null;
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private Long extractUserNoOrNull(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token == null) {
                return null;
            }
            String userId = jwtUtil.getUserId(token);
            if (userId == null || userId.isBlank()) {
                return null;
            }
            var user = iUser.findByUserId(userId.trim());
            return user == null ? null : user.getUserNo();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
