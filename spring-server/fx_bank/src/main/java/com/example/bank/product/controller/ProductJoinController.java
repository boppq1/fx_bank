package com.example.bank.product.controller;

import com.example.bank.gloval.common.ApiResponse;
import com.example.bank.personal.dto.UserEntity;
import com.example.bank.product.dto.ProductJoinCompleteDto;
import com.example.bank.product.dto.ProductJoinFormRequestDto;
import com.example.bank.product.dto.ProductJoinSubmitRequestDto;
import com.example.bank.product.dto.ProductJoinTermsRequestDto;
import com.example.bank.product.dto.ProductTermDto;
import com.example.bank.product.service.ProductJoinService;
import com.example.bank.util.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/product/join")
@RequiredArgsConstructor
public class ProductJoinController {

    private final ProductJoinService productJoinService;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @GetMapping("/{productNo}/terms")
    public ApiResponse<List<ProductTermDto>> getJoinTerms(@PathVariable("productNo") Long productNo) {
        try {
            List<ProductTermDto> terms = productJoinService.getJoinTerms(productNo);
            return ApiResponse.success("가입 약관 조회 성공", terms);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/terms")
    public ApiResponse<Void> saveTerms(
            @RequestBody ProductJoinTermsRequestDto dto,
            HttpSession session
    ) {
        try {
            productJoinService.saveTermsToSession(dto, session);
            return ApiResponse.success("약관 동의 저장 성공", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{productNo}/ocr-success")
    public ApiResponse<Long> saveOcrSuccess(
            @PathVariable("productNo") Long productNo,
            Authentication authentication,
            HttpSession session
    ) {
        try {
            Long userNo = getUserNoFromRedis(authentication);
            Long verificationNo = productJoinService.saveOcrSuccess(productNo, userNo, session);
            return ApiResponse.success("OCR 인증 저장 성공", verificationNo);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/form")
    public ApiResponse<Void> saveJoinForm(
            @RequestBody ProductJoinFormRequestDto dto,
            HttpSession session
    ) {
        try {
            productJoinService.saveJoinFormToSession(dto, session);
            return ApiResponse.success("가입 정보 저장 성공", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/complete")
    public ApiResponse<Long> completeJoin(
            @RequestBody ProductJoinSubmitRequestDto dto,
            Authentication authentication,
            HttpSession session
    ) {
        try {
            Long userNo = getUserNoFromRedis(authentication);
            Long subscriptionNo = productJoinService.completeJoin(dto, userNo, session);
            return ApiResponse.success("상품 가입 완료", subscriptionNo);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/complete/{subscriptionNo}")
    public ApiResponse<ProductJoinCompleteDto> getJoinComplete(@PathVariable("subscriptionNo") Long subscriptionNo) {
        try {
            ProductJoinCompleteDto complete = productJoinService.getJoinComplete(subscriptionNo);
            return ApiResponse.success("가입 완료 정보 조회 성공", complete);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private Long getUserNoFromRedis(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String userId = authentication.getPrincipal().toString();
        if (userId.isBlank() || "anonymousUser".equals(userId)) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String userJson = redisUtil.getData("USER:" + userId);
        if (userJson == null || userJson.isBlank()) {
            throw new IllegalArgumentException("로그인 정보가 만료되었습니다. 다시 로그인해 주세요.");
        }

        try {
            UserEntity user = objectMapper.readValue(userJson, UserEntity.class);
            if (user.getUserNo() == null) {
                throw new IllegalArgumentException("Redis 사용자 정보에 userNo가 없습니다.");
            }
            return user.getUserNo();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Redis 사용자 정보를 읽을 수 없습니다.", e);
        }
    }
}
