package com.example.bank.product.service;

import java.util.List;

import com.example.bank.product.dto.ProductJoinCompleteDto;
import com.example.bank.product.dto.CouponDto;
import com.example.bank.product.dto.CouponSelectionRequestDto;
import com.example.bank.product.dto.ProductJoinEligibilityDto;
import com.example.bank.product.dto.IdentityVerificationRequirementDto;
import com.example.bank.product.dto.ProductJoinResumeDto;
import com.example.bank.product.dto.ProductMySubscriptionDto;
import com.example.bank.product.dto.ProductJoinFormRequestDto;
import com.example.bank.product.dto.ProductJoinSubmitRequestDto;
import com.example.bank.product.dto.ProductJoinTermsRequestDto;
import com.example.bank.product.dto.ProductTermDto;
import com.example.bank.product.dto.WithdrawableForeignAccountDto;

import jakarta.servlet.http.HttpSession;

public interface ProductJoinService {

    // 약관 화면에 보여줄 약관 목록 조회
    List<ProductTermDto> getJoinTerms(Long productNo);

    // 사용자가 체크한 약관을 검증하고 세션에 저장 (+ 임시저장 체크포인트)
    void saveTermsToSession(ProductJoinTermsRequestDto dto, Long userNo, HttpSession session);

    // OCR 인증 성공 결과 저장
    Long saveOcrSuccess(Long productNo, Long userNo, HttpSession session);

    Long saveOcrVerification(
            Long productNo,
            Long userNo,
            boolean ocrSuccess,
            boolean nameMatched,
            boolean birthMatched,
            HttpSession session
    );

    IdentityVerificationRequirementDto getIdentityVerificationRequirement(Long productNo, Long userNo);

    // 가입 정보 입력값을 검증하고 세션에 저장
    void saveJoinFormToSession(ProductJoinFormRequestDto dto, Long userNo, HttpSession session);

    List<WithdrawableForeignAccountDto> getWithdrawableForeignAccounts(Long userNo, String currencyCode);

    List<CouponDto> getAvailableCoupons(Long userNo, Long productNo);

    void saveCouponToSession(CouponSelectionRequestDto dto, Long userNo, HttpSession session);

    // 전자서명 후 최종 가입 저장
    Long completeJoin(ProductJoinSubmitRequestDto dto, Long userNo, HttpSession session);

    // 가입 완료 화면 조회
    List<ProductMySubscriptionDto> getMySubscriptions(Long userNo);

    ProductJoinEligibilityDto getJoinEligibility(Long productNo, Long userNo);

    ProductJoinCompleteDto getJoinComplete(Long subscriptionNo);

    // ===== 임시저장 / 이어서 가입 =====

    /** 재개 가능 여부 + 모달/프리필용 요약 (상품·금리 유효성, 인증 유효성 포함 판정) */
    ProductJoinResumeDto getResumeInfo(Long userNo, Long productNo);

    /** 진행상황을 세션에 복원(약관 + 유효한 OCR 인증만)하고 라우팅/프리필 정보 반환 */
    ProductJoinResumeDto resumeIntoSession(Long userNo, Long productNo, HttpSession session);

    /** 새로 시작: 기존 IN_PROGRESS 진행행을 만료 처리 */
    void discardProgress(Long userNo, Long productNo);
}
