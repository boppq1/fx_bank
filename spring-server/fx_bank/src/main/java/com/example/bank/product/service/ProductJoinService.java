package com.example.bank.product.service;

import java.util.List;

import com.example.bank.product.dto.ProductJoinCompleteDto;
import com.example.bank.product.dto.ProductJoinFormRequestDto;
import com.example.bank.product.dto.ProductJoinSubmitRequestDto;
import com.example.bank.product.dto.ProductJoinTermsRequestDto;
import com.example.bank.product.dto.ProductTermDto;

import jakarta.servlet.http.HttpSession;

public interface ProductJoinService {

    // 약관 화면에 보여줄 약관 목록 조회
    List<ProductTermDto> getJoinTerms(Long productNo);

    // 사용자가 체크한 약관을 검증하고 세션에 저장
    void saveTermsToSession(ProductJoinTermsRequestDto dto, HttpSession session);

    // OCR 인증 성공 결과 저장
    Long saveOcrSuccess(Long productNo, Long userNo, HttpSession session);

    // 가입 정보 입력값을 검증하고 세션에 저장
    void saveJoinFormToSession(ProductJoinFormRequestDto dto, HttpSession session);

    // 전자서명 후 최종 가입 저장
    Long completeJoin(ProductJoinSubmitRequestDto dto, Long userNo, HttpSession session);

    // 가입 완료 화면 조회
    ProductJoinCompleteDto getJoinComplete(Long subscriptionNo);
}