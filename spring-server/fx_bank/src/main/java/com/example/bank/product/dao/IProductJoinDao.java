package com.example.bank.product.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.bank.product.dto.ElectronicSignatureDto;
import com.example.bank.product.dto.ForeignAccountBalanceInsertDto;
import com.example.bank.product.dto.ForeignAccountInsertDto;
import com.example.bank.product.dto.IdVerificationDto;
import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.dto.ProductJoinCompleteDto;
import com.example.bank.product.dto.ProductSubscriptionInsertDto;
import com.example.bank.product.dto.ProductTermDto;

// 상품 가입 과정에서 DB랑 직접 연결되는 메서드 모음
@Mapper
public interface IProductJoinDao {

    // =====================================================
    // 1. 시퀀스 조회
    // =====================================================

    Long selectNextVerificationNo(); // OCR 인증 번호 미리 뽑기

    Long selectNextSubscriptionNo(); // 상품 가입 번호 미리 뽑기

    Long selectNextSignatureNo(); // 전자서명 번호 미리 뽑기

    Long selectNextForeignAccountNo(); // 실제 외화 계좌 PK 발급

    Long selectNextForeignBalanceNo(); // 외화 계좌 통화별 잔액 PK 발급


    // =====================================================
    // 2. 상품 가입 전 검증용 조회
    // =====================================================

    ProductDetailDto selectProductForJoin(@Param("productNo") Long productNo); // 가입할 상품 정보 조회

    int countProductCurrency(
            @Param("productNo") Long productNo,
            @Param("currencyCode") String currencyCode
    ); // 선택한 통화가 해당 상품에서 지원되는지 확인

    int countProductRate(
            @Param("productNo") Long productNo,
            @Param("rateNo") Long rateNo,
            @Param("periodMonth") Integer periodMonth
    ); // 선택한 금리/기간이 해당 상품의 금리 정보와 일치하는지 확인


    // =====================================================
    // 3. 약관 조회 / 약관 검증
    // =====================================================

    List<ProductTermDto> selectJoinTerms(@Param("productNo") Long productNo); // 가입 약관 목록 조회

    int countRequiredTerms(@Param("productNo") Long productNo); // 필수 약관 총 개수 확인

    int countMatchedRequiredTerms(
            @Param("productNo") Long productNo,
            @Param("requiredTermsCodes") List<String> requiredTermsCodes
    ); // 사용자가 체크한 필수 약관이 실제 필수 약관과 일치하는지 확인

    String selectTermsTitle(@Param("termsCode") String termsCode); // 약관 코드로 약관 제목 조회


    // =====================================================
    // 4. OCR 인증 저장 / 검증
    // =====================================================

    int insertIdVerification(IdVerificationDto dto); // OCR 인증 결과 저장

    int countValidVerification(
            @Param("verificationNo") Long verificationNo,
            @Param("userNo") Long userNo,
            @Param("productNo") Long productNo
    ); // 가입 전 OCR 인증 유효성 확인


    // =====================================================
    // 5. 상품 가입 저장
    // =====================================================

    int insertProductSubscription(ProductSubscriptionInsertDto dto); // 최종 상품 가입 정보 저장

    int insertForeignAccount(ForeignAccountInsertDto dto); // 실제 외화 계좌 생성

    int insertForeignAccountBalance(ForeignAccountBalanceInsertDto dto); // 생성된 외화 계좌의 통화별 잔액 생성


    // =====================================================
    // 6. 약관 동의 저장
    // =====================================================

    int insertRequiredTermsAgreement(
            @Param("subscriptionNo") Long subscriptionNo,
            @Param("termsCode") String termsCode
    ); // 필수 약관 동의 저장

    int insertOptionalTermsAgreement(
            @Param("subscriptionNo") Long subscriptionNo,
            @Param("termsCode") String termsCode,
            @Param("termsTitle") String termsTitle
    ); // 선택 약관 동의 저장


    // =====================================================
    // 7. 전자서명 저장
    // =====================================================

    int insertElectronicSignature(ElectronicSignatureDto dto); // 전자서명 저장


    // =====================================================
    // 8. 가입 완료 화면 조회
    // =====================================================

    ProductJoinCompleteDto selectJoinComplete(
            @Param("subscriptionNo") Long subscriptionNo
    ); // 가입 완료 화면 정보 조회
}
