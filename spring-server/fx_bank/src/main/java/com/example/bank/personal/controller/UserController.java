package com.example.bank.personal.controller;

import com.example.bank.gloval.common.ApiResponse;
import com.example.bank.personal.dto.LoginRequestDto;
import com.example.bank.personal.dto.ReauthRequestDto;
import com.example.bank.personal.dto.RegisterRequestDto;
import com.example.bank.personal.service.OcrService;
import com.example.bank.personal.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OcrService ocrService; // 신분증 OCR(FastAPI) 중계
    // 🚨 대통합 완료: JwtUtil과 RedisUtil은 서비스 내부로 숨었으므로 컨트롤러에서 완벽히 제거되었습니다.

    /**
     * [신분증 OCR] 프론트가 보낸 신분증 이미지를 FastAPI(YOLO+CLOVA)로 중계하고 결과 JSON 반환.
     * 응답의 name/rrnMasked/address 등을 회원가입·재인증 폼에 자동 채운다.
     */
    @PostMapping("/ocr/id-card")
    public ApiResponse<Map<String, Object>> ocrIdCard(@RequestParam("file") MultipartFile file) {
        try {
            return ApiResponse.success("신분증 인식 완료", ocrService.recognizeIdCard(file));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * [OCR 재인증] 보관 만료된 사용자가 신분증 재촬영으로 주민번호 재등록.
     * 사용자 식별은 JwtFilter 가 SecurityContext 에 심어둔 userId(principal) 로 한다.
     */
    @PostMapping("/reauth")
    public ApiResponse<Void> reauth(@RequestBody ReauthRequestDto reauthRequest) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            return ApiResponse.error("로그인이 필요합니다.");
        }
        try {
            userService.reauthenticate(principal.toString(), reauthRequest.getRrn(), reauthRequest.getRrnMasked());
            return ApiResponse.success("신분증 재인증이 완료되었습니다.", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/check-id")
    public ApiResponse<Boolean> checkId(@RequestParam("userId") String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return ApiResponse.error("아이디를 입력해 주세요.");
        }
        return ApiResponse.success("아이디 확인 완료", userService.isUserIdDuplicated(userId));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequestDto registerRequest) throws IllegalArgumentException {
        try {
            userService.registerUser(registerRequest);
            return ApiResponse.success("회원가입이 성공적으로 완료되었습니다.", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody LoginRequestDto loginRequest, HttpServletResponse response) {
        try {
            // 🚨 51번 줄 에러 지점 해결: 이제 여러 메서드를 쪼개 부르지 않고, 통합된 login() 딱 하나만 호출합니다.
            Map<String, String> tokenSet = userService.login(loginRequest);

            // 1. 브라우저 쿠키(Refresh Token) 세팅
            String cookieHeader = String.format("refreshToken=%s; Path=/; HttpOnly; SameSite=Lax", tokenSet.get("refreshToken"));
            response.addHeader("Set-Cookie", cookieHeader);

            // 2. 응답 데이터(Access Token + 재인증 필요 여부) 세팅
            Map<String, String> responseData = new HashMap<>();
            responseData.put("accessToken", tokenSet.get("accessToken"));
            // 주민번호 보관 만료(3일 경과)/부재 시 프론트가 OCR 재인증 화면으로 유도
            responseData.put("reauthRequired", tokenSet.getOrDefault("reauthRequired", "false"));

            return ApiResponse.success("로그인이 성공했습니다.", responseData);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> silentRefresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        
        // 1. 쿠키 자체가 없으면 바로 컷
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ApiResponse.error("인증 정보가 없습니다. 다시 로그인해주세요.");
        }

        try {
            // 2. 서비스에 심사 맡기기
            Map<String, String> newTokenSet = userService.refresh(refreshToken);

            // 3. 새로 구운 Refresh Token을 브라우저 쿠키에 다시 밀어넣기 (기간 연장 효과)
            String cookieHeader = String.format("refreshToken=%s; Path=/; HttpOnly; SameSite=Lax", newTokenSet.get("refreshToken"));
            response.addHeader("Set-Cookie", cookieHeader);

            // 4. 자바스크립트가 메모리에 담을 수 있도록 Access Token 반환
            Map<String, String> responseData = new HashMap<>();
            responseData.put("accessToken", newTokenSet.get("accessToken"));

            return ApiResponse.success("토큰 재발급 성공", responseData);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String accessToken, // 🚨 프론트에서 보낸 토큰 받기
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        
        // 1. 서비스에 두 개의 토큰을 모두 넘겨서 폭파 및 블랙리스트 처리 지시
        userService.logout(accessToken, refreshToken);

        // 2. 브라우저 쿠키 폭파 (유효기간 0)
        String deleteCookieHeader = "refreshToken=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";
        response.addHeader("Set-Cookie", deleteCookieHeader);

        return ApiResponse.success("안전하게 로그아웃 되었습니다.", null);
    }
    
}