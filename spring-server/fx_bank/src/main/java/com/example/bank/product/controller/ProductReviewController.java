package com.example.bank.product.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bank.gloval.common.ApiResponse;
import com.example.bank.personal.dto.UserEntity;
import com.example.bank.product.dto.ProductReviewWriteRequestDto;
import com.example.bank.product.service.ProductService;
import com.example.bank.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductService productService;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @GetMapping("/{productNo}/review-eligibility")
    public ApiResponse<Boolean> reviewEligibility(
            @PathVariable("productNo") Long productNo,
            Authentication authentication
    ) {
        try {
            return ApiResponse.success("리뷰 작성 가능 여부 조회 성공",
                    productService.canWriteProductReview(getUserNo(authentication), productNo));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{productNo}/reviews")
    public ApiResponse<Long> writeReview(
            @PathVariable("productNo") Long productNo,
            @RequestBody ProductReviewWriteRequestDto request,
            Authentication authentication
    ) {
        try {
            Long reviewNo = productService.writeProductReview(getUserNo(authentication), productNo, request);
            return ApiResponse.success("리뷰가 등록되었습니다.", reviewNo);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private Long getUserNo(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        String userId = authentication.getPrincipal().toString();
        String userJson = redisUtil.getData("USER:" + userId);
        if (userJson == null || userJson.isBlank()) {
            throw new IllegalArgumentException("로그인 정보가 만료되었습니다. 다시 로그인해주세요.");
        }
        try {
            UserEntity user = objectMapper.readValue(userJson, UserEntity.class);
            if (user.getUserNo() == null) {
                throw new IllegalArgumentException("사용자 정보를 확인할 수 없습니다.");
            }
            return user.getUserNo();
        } catch (Exception e) {
            throw new IllegalArgumentException("사용자 정보를 확인할 수 없습니다.", e);
        }
    }
}
