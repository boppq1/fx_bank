package com.example.bank.product.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.product.constant.JoinProgressStep;
import com.example.bank.product.dao.IProductJoinDao;
import com.example.bank.product.dao.IProductJoinProgressDao;
import com.example.bank.product.dto.ElectronicSignatureDto;
import com.example.bank.product.dto.CouponDto;
import com.example.bank.product.dto.CouponSelectionRequestDto;
import com.example.bank.product.dto.ForeignAccountBalanceInsertDto;
import com.example.bank.product.dto.ForeignAccountInsertDto;
import com.example.bank.product.dto.IdVerificationDto;
import com.example.bank.product.dto.IdentityVerificationRequirementDto;
import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.dto.ProductJoinCompleteDto;
import com.example.bank.product.dto.ProductJoinEligibilityDto;
import com.example.bank.product.dto.ProductJoinProgressDto;
import com.example.bank.product.dto.ProductJoinResumeDto;
import com.example.bank.product.dto.ProductMySubscriptionDto;
import com.example.bank.product.dto.ProductJoinFormRequestDto;
import com.example.bank.product.dto.ProductJoinSubmitRequestDto;
import com.example.bank.product.dto.ProductJoinTermsRequestDto;
import com.example.bank.product.dto.ProductSubscriptionInsertDto;
import com.example.bank.product.dto.ProductTermDto;
import com.example.bank.product.dto.WithdrawableForeignAccountDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductJoinServiceImpl implements ProductJoinService {

    private final IProductJoinDao productJoinDao;
    private final IProductJoinProgressDao progressDao;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.file.signature-dir:uploads/signatures}")
    private String signatureDir;

    private static final String SESSION_TERMS = "PRODUCT_JOIN_TERMS";
    private static final String SESSION_FORM = "PRODUCT_JOIN_FORM";
    private static final String SESSION_VERIFICATION_NO = "PRODUCT_JOIN_VERIFICATION_NO";
    private static final String SESSION_PHONE_VERIFIED_PRODUCT_NO = "PRODUCT_JOIN_PHONE_VERIFIED_PRODUCT_NO";
    private static final String SESSION_COUPON = "PRODUCT_JOIN_COUPON";
    private static final String BANK_NAME = "BUSAN BANK";
    private static final BigDecimal DEFAULT_LIMIT_ONCE = new BigDecimal("1000000");
    private static final BigDecimal DEFAULT_LIMIT_DAILY = new BigDecimal("5000000");

    // =====================================================
    // 1. 약관 조회
    // =====================================================

    @Override
    public List<ProductTermDto> getJoinTerms(Long productNo) {
        if (productNo == null) {
            throw new IllegalArgumentException("상품 번호가 없습니다.");
        }

        ProductDetailDto product = productJoinDao.selectProductForJoin(productNo);
        if (product == null) {
            throw new IllegalArgumentException("가입 가능한 상품이 아닙니다.");
        }

        return productJoinDao.selectJoinTerms(productNo);
    }

    @Override
    public IdentityVerificationRequirementDto getIdentityVerificationRequirement(Long productNo, Long userNo) {
        if (productNo == null || userNo == null) {
            throw new IllegalArgumentException("본인확인 대상 조회에 필요한 정보가 없습니다.");
        }

        ProductDetailDto product = productJoinDao.selectProductForJoin(productNo);
        if (product == null) {
            throw new IllegalArgumentException("가입 가능한 상품이 아닙니다.");
        }

        // 입출금식 외화예금은 CDD 대상이므로 가입 때마다 강화된 신원확인을 요구한다.
        if (requiresOcrVerification(product)) {
            return new IdentityVerificationRequirementDto(
                    true, "CDD", "입출금이 자유로운 외화예금 상품은 신분증 OCR 본인확인이 필요합니다."
            );
        }

        // 정기예금은 원칙적으로 OCR 비대상이지만, 최근 1년간 상품 가입 활동이 없으면 EDD 대상으로 본다.
        Date latestActivity = productJoinDao.selectLatestFinancialProductActivity(userNo);
        Date oneYearAgo = Date.from(LocalDate.now().minusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (latestActivity == null || latestActivity.before(oneYearAgo)) {
            return new IdentityVerificationRequirementDto(
                    true, "EDD", "최근 1년간 금융상품 활동이 없어 추가 신원확인이 필요합니다."
            );
        }

        return new IdentityVerificationRequirementDto(
                false, "PHONE", "정기예금 상품은 휴대폰 본인인증 절차로 진행합니다."
        );
    }

    @Override
    public ProductJoinEligibilityDto getJoinEligibility(Long productNo, Long userNo) {
        if (productNo == null || userNo == null) {
            throw new IllegalArgumentException("가입 가능 여부를 확인할 수 없습니다.");
        }

        ProductDetailDto product = productJoinDao.selectProductForJoin(productNo);
        if (product == null) {
            throw new IllegalArgumentException("가입 가능한 상품이 아닙니다.");
        }

        if (isDemandDepositProduct(product)) {
            return new ProductJoinEligibilityDto(true, "입출금식 외화예금은 바로 가입할 수 있습니다.");
        }

        if (productJoinDao.countWithdrawableSourceAccounts(userNo) == 0) {
            return new ProductJoinEligibilityDto(false,
                    "정기예금·적금 가입 전에는 출금 가능한 입출금 계좌가 필요합니다. 외화 입출금 예금부터 가입해주세요.");
        }

        return new ProductJoinEligibilityDto(true, "출금 계좌를 확인했습니다.");
    }

    // =====================================================
    // 2. 약관 동의 세션 저장
    // =====================================================

    @Override
    public void saveTermsToSession(ProductJoinTermsRequestDto dto, Long userNo, HttpSession session) {
        if (dto == null || dto.getProductNo() == null) {
            throw new IllegalArgumentException("약관 동의 정보가 없습니다.");
        }

        Long productNo = dto.getProductNo();

        ProductDetailDto product = productJoinDao.selectProductForJoin(productNo);
        if (product == null) {
            throw new IllegalArgumentException("가입 가능한 상품이 아닙니다.");
        }

        List<String> requiredTermsCodes = safeList(dto.getRequiredTermsCodes());
        List<String> optionalTermsCodes = safeList(dto.getOptionalTermsCodes());

        int requiredTotalCount = productJoinDao.countRequiredTerms(productNo);
        int matchedRequiredCount = productJoinDao.countMatchedRequiredTerms(productNo, requiredTermsCodes);

        if (requiredTotalCount != matchedRequiredCount) {
            throw new IllegalArgumentException("필수 약관에 모두 동의해야 합니다.");
        }

        ProductJoinTermsRequestDto sessionDto = new ProductJoinTermsRequestDto();
        sessionDto.setProductNo(productNo);
        sessionDto.setRequiredTermsCodes(requiredTermsCodes);
        sessionDto.setOptionalTermsCodes(optionalTermsCodes);

        session.setAttribute(SESSION_TERMS, sessionDto);

        // 임시저장 체크포인트 (TERMS)
        saveProgressSnapshot(userNo, productNo, JoinProgressStep.TERMS, session);
    }

    // =====================================================
    // 3. OCR 인증 결과 저장
    // =====================================================

    @Override
    @Transactional
    public Long saveOcrSuccess(Long productNo, Long userNo, HttpSession session) {
        if (productNo == null) {
            throw new IllegalArgumentException("상품 번호가 없습니다.");
        }

        if (userNo == null) {
            throw new IllegalArgumentException("사용자 정보가 없습니다.");
        }

        ProductDetailDto product = productJoinDao.selectProductForJoin(productNo);
        if (product == null) {
            throw new IllegalArgumentException("가입 가능한 상품이 아닙니다.");
        }

        Long verificationNo = productJoinDao.selectNextVerificationNo();

        Date now = new Date();
        Date expiredDt = Date.from(
                LocalDateTime.now()
                        .plusMinutes(30)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );

        IdVerificationDto verificationDto = new IdVerificationDto();
        verificationDto.setVerificationNo(verificationNo);
        verificationDto.setUserNo(userNo);
        verificationDto.setProductNo(productNo);
        verificationDto.setVerificationStatus("성공");
        verificationDto.setVerificationMethod("OCR");
        verificationDto.setOcrProvider("LOCAL_TEST");
        verificationDto.setMatchedNameYn("Y");
        verificationDto.setMatchedBirthYn("Y");
        verificationDto.setFailReason(null);
        verificationDto.setVerifiedDt(now);
        verificationDto.setExpiredDt(expiredDt);

        productJoinDao.insertIdVerification(verificationDto);

        session.setAttribute(SESSION_VERIFICATION_NO, verificationNo);

        // 임시저장 체크포인트 (VERIFY)
        saveProgressSnapshot(userNo, productNo, JoinProgressStep.VERIFY, session);

        return verificationNo;
    }

    @Override
    @Transactional
    public Long saveOcrVerification(
            Long productNo,
            Long userNo,
            boolean ocrSuccess,
            boolean nameMatched,
            boolean birthMatched,
            HttpSession session
    ) {
        if (productNo == null || userNo == null) {
            throw new IllegalArgumentException("OCR 인증에 필요한 사용자 또는 상품 정보가 없습니다.");
        }
        if (!ocrSuccess) {
            throw new IllegalArgumentException("신분증 OCR 인식에 실패했습니다. 이미지가 선명한지 확인해주세요.");
        }
        if (!nameMatched || !birthMatched) {
            throw new IllegalArgumentException("신분증 정보가 로그인 회원 정보와 일치하지 않습니다.");
        }
        if (productJoinDao.selectProductForJoin(productNo) == null) {
            throw new IllegalArgumentException("가입 가능한 상품이 아닙니다.");
        }

        Long verificationNo = productJoinDao.selectNextVerificationNo();
        Date now = new Date();
        Date expiredDt = Date.from(LocalDateTime.now().plusMinutes(30)
                .atZone(ZoneId.systemDefault()).toInstant());

        IdVerificationDto verificationDto = new IdVerificationDto();
        verificationDto.setVerificationNo(verificationNo);
        verificationDto.setUserNo(userNo);
        verificationDto.setProductNo(productNo);
        verificationDto.setVerificationStatus("성공");
        verificationDto.setVerificationMethod("OCR");
        verificationDto.setOcrProvider("FASTAPI_YOLO_CLOVA");
        verificationDto.setMatchedNameYn("Y");
        verificationDto.setMatchedBirthYn("Y");
        verificationDto.setVerifiedDt(now);
        verificationDto.setExpiredDt(expiredDt);
        productJoinDao.insertIdVerification(verificationDto);

        session.setAttribute(SESSION_VERIFICATION_NO, verificationNo);

        // 임시저장 체크포인트 (VERIFY)
        saveProgressSnapshot(userNo, productNo, JoinProgressStep.VERIFY, session);
        return verificationNo;
    }

    // =====================================================
    // 4. 가입 정보 세션 저장
    // =====================================================

    @Override
    public void saveJoinFormToSession(ProductJoinFormRequestDto dto, Long userNo, HttpSession session) {
        if (dto == null || dto.getProductNo() == null) {
            throw new IllegalArgumentException("가입 정보가 없습니다.");
        }

        if (dto.getCurrencyCode() == null || dto.getCurrencyCode().isBlank()) {
            throw new IllegalArgumentException("통화 정보가 없습니다.");
        }

        Long productNo = dto.getProductNo();

        ProductDetailDto product = productJoinDao.selectProductForJoin(productNo);
        if (product == null) {
            throw new IllegalArgumentException("가입 가능한 상품이 아닙니다.");
        }

        if (isDemandDepositProduct(product)) {
            // 기간이 없는 예금/통장 개설형 상품은 가입 기간, 출금 계좌, 가입 금액을 받지 않는다.
            // product_subscriptions.period_month가 NOT NULL이라 DB에는 0개월로 저장한다.
            dto.setAmount(BigDecimal.ZERO);
            dto.setWithdrawalAccountNo(null);
            dto.setRateNo(null);
            dto.setPeriodMonth(0);
            dto.setAppliedRate(product.getBaseRate() == null ? BigDecimal.ZERO : product.getBaseRate());
        } else {
            if (dto.getRateNo() == null) {
                throw new IllegalArgumentException("금리 정보를 선택해주세요.");
            }
            if (dto.getPeriodMonth() == null || dto.getPeriodMonth() <= 0) {
                throw new IllegalArgumentException("가입 기간을 확인해주세요.");
            }
            if (dto.getAppliedRate() == null) {
                throw new IllegalArgumentException("적용 금리가 없습니다.");
            }
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("가입 금액을 확인해주세요.");
            }
            if (dto.getWithdrawalAccountNo() == null || dto.getWithdrawalAccountNo().isBlank()) {
                throw new IllegalArgumentException("예금/적금 가입에 사용할 출금 계좌를 선택해주세요.");
            }
            boolean ownsSourceAccount = getWithdrawableForeignAccounts(userNo, dto.getCurrencyCode()).stream()
                    .anyMatch(account -> dto.getWithdrawalAccountNo().equals(account.getAccountNo()));
            if (!ownsSourceAccount) {
                throw new IllegalArgumentException("선택한 출금 계좌를 사용할 수 없습니다.");
            }
        }

        int currencyCount = productJoinDao.countProductCurrency(productNo, dto.getCurrencyCode());
        if (currencyCount == 0) {
            throw new IllegalArgumentException("해당 상품에서 지원하지 않는 통화입니다.");
        }

        if (!isDemandDepositProduct(product)) {
            int rateCount = productJoinDao.countProductRate(
                    productNo,
                    dto.getRateNo(),
                    dto.getPeriodMonth()
            );

            if (rateCount == 0) {
                throw new IllegalArgumentException("해당 상품의 금리 정보가 올바르지 않습니다.");
            }
        }

        if (dto.getAccountPassword() == null || dto.getAccountPassword().isBlank()) {
            throw new IllegalArgumentException("계좌 비밀번호를 입력해주세요.");
        }

        session.setAttribute(SESSION_FORM, dto);

        // 임시저장 체크포인트(FORM)
        saveProgressSnapshot(userNo, productNo, JoinProgressStep.FORM, session);
    }
    @Override
    public List<WithdrawableForeignAccountDto> getWithdrawableForeignAccounts(Long userNo, String currencyCode) {
        if (userNo == null || currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException("출금 계좌 조회에 필요한 정보가 없습니다.");
        }
        return productJoinDao.selectWithdrawableForeignAccounts(userNo, currencyCode);
    }

    @Override
    public List<CouponDto> getAvailableCoupons(Long userNo, Long productNo) {
        if (userNo == null || productNo == null) {
            throw new IllegalArgumentException("쿠폰 조회에 필요한 정보가 없습니다.");
        }
        return productJoinDao.selectAvailableCoupons(userNo, productNo);
    }

    @Override
    public void saveCouponToSession(CouponSelectionRequestDto dto, Long userNo, HttpSession session) {
        if (dto == null || dto.getProductNo() == null) {
            throw new IllegalArgumentException("쿠폰 선택 정보가 없습니다.");
        }
        if (dto.getCouponNo() == null) {
            session.removeAttribute(SESSION_COUPON);
        } else {
            CouponDto coupon = productJoinDao.selectAvailableCouponByNo(dto.getCouponNo(), userNo, dto.getProductNo());
            if (coupon == null) {
                throw new IllegalArgumentException("사용할 수 있는 우대금리 쿠폰이 아닙니다.");
            }
            session.setAttribute(SESSION_COUPON, coupon);
        }

        // 임시저장 체크포인트 (COUPON) — 쿠폰 자체는 progress 에 저장하지 않음(이어서 시 재선택)
        saveProgressSnapshot(userNo, dto.getProductNo(), JoinProgressStep.COUPON, session);
    }

    // =====================================================
    // 5. 최종 가입 저장
    // =====================================================

    @Override
    @Transactional
    public Long completeJoin(ProductJoinSubmitRequestDto dto, Long userNo, HttpSession session) {
        // DB 세션의 병렬 DML이 활성화된 경우 같은 테이블의 UPDATE 후 INSERT가 ORA-12838로 막힌다.
        // 이 트랜잭션에서만 병렬 DML을 끄고 출금·입금 처리를 하나의 원자적 작업으로 수행한다.
        productJoinDao.disableParallelDml();

        if (dto == null || dto.getProductNo() == null) {
            throw new IllegalArgumentException("최종 가입 정보가 없습니다.");
        }

        if (userNo == null) {
            throw new IllegalArgumentException("사용자 정보가 없습니다.");
        }

        if (dto.getSignatureImageData() == null || dto.getSignatureImageData().isBlank()) {
            throw new IllegalArgumentException("전자서명이 필요합니다.");
        }

        ProductJoinTermsRequestDto termsDto =
                (ProductJoinTermsRequestDto) session.getAttribute(SESSION_TERMS);

        ProductJoinFormRequestDto formDto =
                (ProductJoinFormRequestDto) session.getAttribute(SESSION_FORM);

        CouponDto selectedCoupon =
                (CouponDto) session.getAttribute(SESSION_COUPON);

        Long verificationNo =
                (Long) session.getAttribute(SESSION_VERIFICATION_NO);

        if (termsDto == null) {
            throw new IllegalArgumentException("약관 동의 정보가 없습니다.");
        }

        if (formDto == null) {
            throw new IllegalArgumentException("가입 정보가 없습니다.");
        }

        if (verificationNo == null && getIdentityVerificationRequirement(dto.getProductNo(), userNo).isRequired()) {
            throw new IllegalArgumentException("OCR 인증 정보가 없습니다.");
        }

        Long productNo = dto.getProductNo();

        if (!productNo.equals(termsDto.getProductNo()) || !productNo.equals(formDto.getProductNo())) {
            throw new IllegalArgumentException("가입 상품 정보가 일치하지 않습니다.");
        }

        ProductDetailDto product = productJoinDao.selectProductForJoin(productNo);
        if (product == null) {
            throw new IllegalArgumentException("가입 가능한 상품이 아닙니다.");
        }


        if (isDemandDepositProduct(product)) {
            formDto.setAmount(BigDecimal.ZERO);
            formDto.setWithdrawalAccountNo(null);
            formDto.setRateNo(null);
            formDto.setPeriodMonth(0);
            formDto.setAppliedRate(product.getBaseRate() == null ? BigDecimal.ZERO : product.getBaseRate());
        } else if (formDto.getAmount() == null || formDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("가입 금액을 확인해주세요.");
        }

        BigDecimal appliedRate = formDto.getAppliedRate();
        if (selectedCoupon != null) {
            // 화면에서 골랐던 쿠폰도 최종 저장 직전에 다시 확인해 이미 사용된 쿠폰의 중복 적용을 막는다.
            CouponDto availableCoupon = productJoinDao.selectAvailableCouponByNo(
                    selectedCoupon.getCouponNo(), userNo, productNo
            );
            if (availableCoupon == null) {
                throw new IllegalArgumentException("선택한 우대금리 쿠폰을 사용할 수 없습니다.");
            }

            BigDecimal preferentialRate = availableCoupon.getPreferentialRate() == null
                    ? BigDecimal.ZERO : availableCoupon.getPreferentialRate();
            appliedRate = appliedRate.add(preferentialRate);

            // 상품이 정한 최고 금리를 넘지 않도록 제한한다.
            if (product.getMaxRate() != null && appliedRate.compareTo(product.getMaxRate()) > 0) {
                appliedRate = product.getMaxRate();
            }

            if (productJoinDao.markCouponUsed(availableCoupon.getCouponNo(), userNo) == 0) {
                throw new IllegalArgumentException("우대금리 쿠폰이 이미 사용되었습니다.");
            }
        }

        ProductJoinEligibilityDto joinEligibility = getJoinEligibility(productNo, userNo);
        if (!joinEligibility.isCanJoin()) {
            throw new IllegalArgumentException(joinEligibility.getReason());
        }

        int activeRrnSubscriptionCount = productJoinDao.countActiveProductSubscriptionBySameRrn(userNo, productNo);
        if (activeRrnSubscriptionCount > 0) {
            throw new IllegalArgumentException("이미 동일한 주민등록번호로 가입된 상품입니다. 내 가입 상품에서 가입 내역을 확인해주세요.");
        }

        int activeSubscriptionCount = productJoinDao.countActiveProductSubscription(userNo, productNo);
        if (activeSubscriptionCount > 0) {
            throw new IllegalArgumentException("이미 가입한 상품입니다. 내 가입 상품에서 계좌 정보를 확인해주세요.");
        }

        IdentityVerificationRequirementDto identityRequirement =
                getIdentityVerificationRequirement(productNo, userNo);
        if (identityRequirement.isRequired()) {
            int validVerificationCount = productJoinDao.countValidVerification(
                    verificationNo,
                    userNo,
                    productNo
            );

            if (validVerificationCount == 0) {
                throw new IllegalArgumentException("유효한 OCR 인증 정보가 없습니다.");
            }
        } else if (!productNo.equals(session.getAttribute(SESSION_PHONE_VERIFIED_PRODUCT_NO))) {
            throw new IllegalArgumentException("가입하기 전에 휴대폰 본인인증을 완료해주세요.");
        }

        if (!isDemandDepositProduct(product)) {
            int withdrawnCount = productJoinDao.withdrawForeignAccountBalance(
                    userNo,
                    formDto.getWithdrawalAccountNo(),
                    formDto.getCurrencyCode(),
                    formDto.getAmount()
            );
            if (withdrawnCount == 0) {
                throw new IllegalArgumentException("출금 계좌의 잔액이 부족하거나 사용할 수 없습니다.");
            }
        }

        // 실제 외화 계좌를 먼저 생성한다.
        // product_subscriptions.acnt_no에만 번호를 넣으면 계좌 테이블에는 아무것도 남지 않기 때문에,
        // 가입 완료 트랜잭션 안에서 foreign_accounts와 foreign_account_balances까지 함께 저장한다.
        Long fxAcntNo = productJoinDao.selectNextForeignAccountNo();
        String accountNo = generateAccountNo(fxAcntNo, product);

        ForeignAccountInsertDto foreignAccountDto = new ForeignAccountInsertDto();
        foreignAccountDto.setFxAcntNo(fxAcntNo);
        foreignAccountDto.setAcntPw(passwordEncoder.encode(formDto.getAccountPassword()));
        foreignAccountDto.setBankName(BANK_NAME);
        foreignAccountDto.setAccountNo(accountNo);
        foreignAccountDto.setUserNo(userNo);
        foreignAccountDto.setLimitOnce(DEFAULT_LIMIT_ONCE);
        foreignAccountDto.setLimitDaily(DEFAULT_LIMIT_DAILY);

        productJoinDao.insertForeignAccount(foreignAccountDto);

        // foreign_accounts는 계좌 마스터이고, 실제 통화별 잔액은 foreign_account_balances에서 관리한다.
        Long balanceNo = productJoinDao.selectNextForeignBalanceNo();

        ForeignAccountBalanceInsertDto balanceDto = new ForeignAccountBalanceInsertDto();
        balanceDto.setBalanceNo(balanceNo);
        balanceDto.setFxAcntId(fxAcntNo);
        balanceDto.setCurrencyCode(formDto.getCurrencyCode());
        balanceDto.setBalance(formDto.getAmount());

        productJoinDao.insertForeignAccountBalance(balanceDto);

        Long subscriptionNo = productJoinDao.selectNextSubscriptionNo();

        ProductSubscriptionInsertDto subscriptionDto = new ProductSubscriptionInsertDto();
        subscriptionDto.setSubscriptionNo(subscriptionNo);
        subscriptionDto.setProductNo(productNo);
        subscriptionDto.setUserNo(userNo);
        subscriptionDto.setRateNo(formDto.getRateNo());
        subscriptionDto.setType(resolveSubscriptionType(product));
        // 상품 가입 정보에는 방금 만든 실제 외화 계좌번호를 연결한다.
        // 현재 DDL에 FK는 없지만, foreign_accounts.account_no와 같은 값을 넣어 업무적으로 연결한다.
        subscriptionDto.setAcntNo(accountNo);
        subscriptionDto.setCurrencyCode(formDto.getCurrencyCode());
        subscriptionDto.setAmount(formDto.getAmount());
        subscriptionDto.setPeriodMonth(formDto.getPeriodMonth());
        subscriptionDto.setVerificationNo(verificationNo);
        subscriptionDto.setSubscriptionStatus("가입완료");
        subscriptionDto.setMaturityDt(isDemandDepositProduct(product) ? null : calculateMaturityDate(formDto.getPeriodMonth()));
        subscriptionDto.setAppliedRate(appliedRate);
        subscriptionDto.setRateChangedDt(new Date());

        productJoinDao.insertProductSubscription(subscriptionDto);

        // 필수 약관 동의 저장
        for (String termsCode : safeList(termsDto.getRequiredTermsCodes())) {
            productJoinDao.insertRequiredTermsAgreement(subscriptionNo, termsCode);
        }

        // 선택 약관 동의 저장
        for (String termsCode : safeList(termsDto.getOptionalTermsCodes())) {
            String termsTitle = productJoinDao.selectTermsTitle(termsCode);

            if (termsTitle != null) {
                productJoinDao.insertOptionalTermsAgreement(
                        subscriptionNo,
                        termsCode,
                        termsTitle
                );
            }
        }

        // 전자서명 이미지 저장
        Long signatureNo = productJoinDao.selectNextSignatureNo();
        String signaturePath = saveSignatureImage(subscriptionNo, dto.getSignatureImageData());

        ElectronicSignatureDto signatureDto = new ElectronicSignatureDto();
        signatureDto.setSignatureNo(signatureNo);
        signatureDto.setSubscriptionNo(subscriptionNo);
        signatureDto.setUserNo(userNo);
        signatureDto.setVerificationNo(verificationNo);
        signatureDto.setSignaturePath(signaturePath);
        signatureDto.setSignedContent(makeSignedContent(subscriptionNo, productNo, formDto, termsDto, appliedRate));
        signatureDto.setSignedDt(new Date());

        productJoinDao.insertElectronicSignature(signatureDto);

        // 임시저장 진행행을 완료로 마킹(감사용 보존). 미러 작업 실패가 가입 확정을 막지 않도록 best-effort.
        try {
            progressDao.markCompleted(userNo, productNo);
        } catch (Exception e) {
            System.err.println("[progress] 완료 마킹 실패(무시): " + e.getMessage());
        }

        clearJoinSession(session);

        return subscriptionNo;
    }

    // =====================================================
    // 6. 가입 완료 조회
    // =====================================================

    @Override
    public ProductJoinCompleteDto getJoinComplete(Long subscriptionNo) {
        if (subscriptionNo == null) {
            throw new IllegalArgumentException("가입 번호가 없습니다.");
        }

        return productJoinDao.selectJoinComplete(subscriptionNo);
    }

    @Override
    public List<ProductMySubscriptionDto> getMySubscriptions(Long userNo) {
        if (userNo == null) {
            throw new IllegalArgumentException("로그인 사용자 정보가 없습니다.");
        }

        return productJoinDao.selectMySubscriptions(userNo);
    }

    // =====================================================
    // 내부 메서드
    // =====================================================

    private List<String> safeList(List<String> list) {
        if (list == null) {
            return new ArrayList<>();
        }

        return list;
    }

    private Date calculateMaturityDate(Integer periodMonth) {
        LocalDate maturityDate = LocalDate.now().plusMonths(periodMonth);

        return Date.from(
                maturityDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );
    }

    // 실제 외화 계좌번호를 은행 계좌처럼 보이는 형식으로 만든다.
    // 333은 프로젝트용 은행 코드, 02는 입출금식, 03은 정기예금식 상품을 의미한다.
    private String generateAccountNo(Long fxAcntNo, ProductDetailDto product) {
        String accountTypeCode = isDemandDepositProduct(product) ? "02" : "03"; // 02는 입출금식 통장 상품, 03은 정기 예금 상품
        String seq = String.format("%07d", fxAcntNo % 10_000_000);

        return "333-" + accountTypeCode + "-" + seq;
    }

    private boolean isDemandDepositProduct(ProductDetailDto product) {
        String productText = productKeywordText(product);
        if (productText.isBlank()) {
            return false;
        }

        // 적금/정기예금은 약정 기간을 가진 상품이므로 통장형 상품에서 제외한다.
        if (productText.contains("적금") || productText.contains("정기")) {
            return false;
        }

        // 정기/적금이 아닌 예금은 입출금식 통장 상품으로 본다.
        // DB에 기간 값이 잘못 들어와도 통장/입출금/일반 예금은 가입기간과 가입금액을 받지 않는다.
        return productText.contains("통장")
                || productText.contains("입출금")
                || productText.contains("예금")
                || !hasJoinPeriod(product);
    }

    private String productKeywordText(ProductDetailDto product) {
        if (product == null) {
            return "";
        }

        String productType = product.getProductType() == null ? "" : product.getProductType();
        String productName = product.getProductName() == null ? "" : product.getProductName();
        return productType + " " + productName;
    }

    private boolean hasJoinPeriod(ProductDetailDto product) {
        if (product == null) {
            return false;
        }
        return (product.getMinPeriodMonth() != null && product.getMinPeriodMonth() > 0)
                || (product.getMaxPeriodMonth() != null && product.getMaxPeriodMonth() > 0);
    }

    private boolean requiresOcrVerification(ProductDetailDto product) {
        String productText = productKeywordText(product);
        if (productText.isBlank()) {
            return false;
        }

        // 정기예금/적금은 기본적으로 휴대폰 인증, 통장형 입출금 상품은 신분증 OCR 인증 대상이다.
        if (productText.contains("정기") || productText.contains("적금")) {
            return false;
        }

        return productText.contains("예금")
                || productText.contains("통장")
                || productText.contains("입출금");
    }

    // 상품명과 products.product_type을 함께 보고 가입 유형을 결정한다.
    private String resolveSubscriptionType(ProductDetailDto product) {
        String productText = productKeywordText(product);
        if (productText.isBlank()) {
            throw new IllegalArgumentException("상품 유형이 없어 가입 유형을 결정할 수 없습니다.");
        }
        if (productText.contains("적금")) {
            return "FOREIGN_SAVINGS";
        }
        if (isDemandDepositProduct(product)) {
            return "FOREIGN_DEMAND_DEPOSIT";
        }
        if (productText.contains("정기") || productText.contains("예금")) {
            return "FOREIGN_TIME_DEPOSIT";
        }
        throw new IllegalArgumentException("지원하지 않는 외화 상품 유형입니다: " + productText);
    }    private String saveSignatureImage(Long subscriptionNo, String signatureImageData) {
        try {
            String base64Data = signatureImageData;

            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            Path directoryPath = Path.of(signatureDir);
            Files.createDirectories(directoryPath);

            String fileName = "signature_" + subscriptionNo + "_" + System.currentTimeMillis() + ".png";
            Path filePath = directoryPath.resolve(fileName);

            Files.write(filePath, imageBytes);

            return filePath.toString().replace("\\", "/");

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("전자서명 이미지 형식이 올바르지 않습니다.", e);
        } catch (IOException e) {
            throw new RuntimeException("전자서명 이미지 저장 중 오류가 발생했습니다.", e);
        }
    }

    private String makeSignedContent(
            Long subscriptionNo,
            Long productNo,
            ProductJoinFormRequestDto formDto,
            ProductJoinTermsRequestDto termsDto,
            BigDecimal appliedRate
    ) {
        Map<String, Object> signedContent = new LinkedHashMap<>();

        signedContent.put("subscriptionNo", subscriptionNo);
        signedContent.put("productNo", productNo);
        signedContent.put("rateNo", formDto.getRateNo());
        signedContent.put("currencyCode", formDto.getCurrencyCode());
        signedContent.put("amount", formDto.getAmount());
        signedContent.put("periodMonth", formDto.getPeriodMonth());
        signedContent.put("appliedRate", appliedRate);
        signedContent.put("requiredTermsCodes", safeList(termsDto.getRequiredTermsCodes()));
        signedContent.put("optionalTermsCodes", safeList(termsDto.getOptionalTermsCodes()));
        signedContent.put("signedAt", LocalDateTime.now().toString());

        try {
            return objectMapper.writeValueAsString(signedContent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("전자서명 내용 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void clearJoinSession(HttpSession session) {
        session.removeAttribute(SESSION_TERMS);
        session.removeAttribute(SESSION_FORM);
        session.removeAttribute(SESSION_VERIFICATION_NO);
        session.removeAttribute(SESSION_COUPON);
        // 완료 후 휴대폰 인증 플래그가 다음 가입 재개에 끼지 않도록 함께 정리
        session.removeAttribute(SESSION_PHONE_VERIFIED_PRODUCT_NO);
    }

    // =====================================================
    // 임시저장 / 이어서 가입
    // =====================================================

    /**
     * 현재 세션 누적분을 스냅샷으로 만들어 product_join_progress 에 저장 (best-effort: 실패해도 라이브 흐름 유지).
     * 정상은 MERGE upsert. (user_no, product_no) 부분 유니크 인덱스가 적용된 환경에서
     * 동시 INSERT 충돌(ORA-00001 → DuplicateKeyException)이 나면 UPDATE 로 폴백한다.
     * 인덱스가 없는 환경에서는 충돌이 발생하지 않으므로 동작 차이가 없다(기존 재개-시-중복정리 방어 유지).
     */
    private void saveProgressSnapshot(Long userNo, Long productNo, JoinProgressStep step, HttpSession session) {
        if (userNo == null || productNo == null) {
            return;
        }

        ProductJoinProgressDto p;
        try {
            p = buildProgressSnapshot(userNo, productNo, step, session);
        } catch (Exception e) {
            System.err.println("[progress] 스냅샷 생성 실패(무시) step=" + step + " : " + e.getMessage());
            return;
        }

        try {
            progressDao.upsertProgress(p); // 정상 경로: MERGE
        } catch (DuplicateKeyException dup) {
            // 부분 유니크 인덱스 적용 환경에서 동시 INSERT 충돌 → UPDATE 폴백
            try {
                progressDao.updateInProgress(p);
            } catch (Exception e2) {
                System.err.println("[progress] 동시성 폴백 UPDATE 실패(무시) step=" + step + " : " + e2.getMessage());
            }
        } catch (Exception e) {
            // 그 밖의 미러 저장 실패도 세션 기반 가입 흐름을 막지 않도록 무시(로그만)
            System.err.println("[progress] 체크포인트 저장 실패(무시) step=" + step + " : " + e.getMessage());
        }
    }

    /** 세션 누적분 → product_join_progress 스냅샷 DTO (계좌비번/출금계좌·쿠폰은 미포함). */
    private ProductJoinProgressDto buildProgressSnapshot(Long userNo, Long productNo, JoinProgressStep step, HttpSession session) {
        ProductJoinProgressDto p = new ProductJoinProgressDto();
        p.setUserNo(userNo);
        p.setProductNo(productNo);
        p.setCurrentStep(step.name());
        p.setProgressStatus(JoinProgressStep.STATUS_IN_PROGRESS);

        ProductJoinTermsRequestDto terms = (ProductJoinTermsRequestDto) session.getAttribute(SESSION_TERMS);
        if (terms != null) {
            p.setRequiredTermsCodes(toJsonArray(terms.getRequiredTermsCodes()));
            p.setOptionalTermsCodes(toJsonArray(terms.getOptionalTermsCodes()));
        }

        p.setVerificationNo((Long) session.getAttribute(SESSION_VERIFICATION_NO));

        ProductJoinFormRequestDto form = (ProductJoinFormRequestDto) session.getAttribute(SESSION_FORM);
        if (form != null) {
            p.setRateNo(form.getRateNo());
            p.setCurrencyCode(form.getCurrencyCode());
            p.setAmount(form.getAmount());
            p.setPeriodMonth(form.getPeriodMonth());
            // 참고용 저장(쿠폰 우대분 섞였을 수 있음). 재개 표시 금리로는 쓰지 않는다.
            p.setAppliedRate(form.getAppliedRate());
        }
        return p;
    }

    @Override
    public ProductJoinResumeDto getResumeInfo(Long userNo, Long productNo) {
        if (userNo == null || productNo == null) {
            return ProductJoinResumeDto.notAvailable();
        }

        ProductJoinProgressDto progress = progressDao.selectLatestInProgress(userNo, productNo);
        if (progress == null) {
            return ProductJoinResumeDto.notAvailable();
        }

        // C 방어: 중복 IN_PROGRESS 정리(최신만 유지)
        try {
            progressDao.expireOtherInProgress(userNo, productNo, progress.getJoinProgressNo());
        } catch (Exception ignore) {
            // 정리 실패는 무시
        }

        // B: 상품 판매 유효성 (selectProductForJoin 은 active='Y' 만 반환)
        ProductDetailDto product = productJoinDao.selectProductForJoin(productNo);
        if (product == null) {
            progressDao.expireAllInProgress(userNo, productNo);
            return ProductJoinResumeDto.notAvailable();
        }

        // B: 금리 유효성 (폼까지 진행해 rate_no 가 있는 경우만)
        if (progress.getRateNo() != null && progress.getPeriodMonth() != null
                && productJoinDao.countProductRate(productNo, progress.getRateNo(), progress.getPeriodMonth()) == 0) {
            progressDao.expireAllInProgress(userNo, productNo);
            return ProductJoinResumeDto.notAvailable();
        }

        ProductJoinResumeDto dto = new ProductJoinResumeDto();
        dto.setAvailable(true);
        dto.setProductNo(productNo);
        dto.setProductName(product.getProductName());
        dto.setCurrentStep(progress.getCurrentStep());
        dto.setRateNo(progress.getRateNo());
        dto.setCurrencyCode(progress.getCurrencyCode());
        dto.setAmount(progress.getAmount());
        dto.setPeriodMonth(progress.getPeriodMonth());
        dto.setUpdatedDt(progress.getUpdatedDt() != null ? progress.getUpdatedDt() : progress.getCreatedDt());
        dto.setExpiredDt(progress.getExpiredDt());

        // A: 표시 금리는 rate_no 기본금리(저장된 applied_rate 가 아님)
        if (progress.getRateNo() != null) {
            dto.setBaseRate(progressDao.selectRateInterest(productNo, progress.getRateNo()));
        }

        // D: 인증 유효성을 폼/쿠폰보다 먼저 판정해 라우팅
        IdentityVerificationRequirementDto requirement = getIdentityVerificationRequirement(productNo, userNo);
        boolean identityRequired = requirement.isRequired();
        boolean identityVerified = progress.getVerificationNo() != null
                && productJoinDao.countValidVerification(progress.getVerificationNo(), userNo, productNo) > 0;
        dto.setIdentityRequired(identityRequired);
        dto.setIdentityVerified(identityVerified);

        if (identityRequired && !identityVerified) {
            dto.setRouteStep("VERIFY");
            dto.setRouteUrl("/product/join/" + productNo + "/form"); // 본인인증은 폼 화면에서 수행
        } else {
            dto.setRouteStep("COUPON");
            dto.setRouteUrl("/product/join/" + productNo + "/coupon");
        }

        return dto;
    }

    @Override
    public ProductJoinResumeDto resumeIntoSession(Long userNo, Long productNo, HttpSession session) {
        ProductJoinResumeDto info = getResumeInfo(userNo, productNo);
        if (!info.isAvailable()) {
            throw new IllegalArgumentException("이어서 진행할 가입 정보가 없습니다. 처음부터 진행해주세요.");
        }

        ProductJoinProgressDto progress = progressDao.selectLatestInProgress(userNo, productNo);
        if (progress == null) {
            throw new IllegalArgumentException("이어서 진행할 가입 정보가 없습니다. 처음부터 진행해주세요.");
        }

        // 약관(비민감) 복원 — 세션 활성 작업본으로 주입
        ProductJoinTermsRequestDto terms = new ProductJoinTermsRequestDto();
        terms.setProductNo(productNo);
        terms.setRequiredTermsCodes(fromJsonArray(progress.getRequiredTermsCodes()));
        terms.setOptionalTermsCodes(fromJsonArray(progress.getOptionalTermsCodes()));
        session.setAttribute(SESSION_TERMS, terms);

        // OCR 인증은 "유효할 때만" 복원 (만료/무효면 재인증 유도)
        if (info.isIdentityVerified() && progress.getVerificationNo() != null) {
            session.setAttribute(SESSION_VERIFICATION_NO, progress.getVerificationNo());
        } else {
            session.removeAttribute(SESSION_VERIFICATION_NO);
        }

        // 민감/시간민감 항목은 복원하지 않음: 폼(계좌비번 포함)·쿠폰·휴대폰인증은 재입력/재선택
        session.removeAttribute(SESSION_FORM);
        session.removeAttribute(SESSION_COUPON);
        session.removeAttribute(SESSION_PHONE_VERIFIED_PRODUCT_NO);

        return info; // routeStep/routeUrl + 프리필(rateNo/currency/amount/period/baseRate)
    }

    @Override
    public void discardProgress(Long userNo, Long productNo) {
        if (userNo == null || productNo == null) {
            return;
        }
        progressDao.expireAllInProgress(userNo, productNo);
    }

    private String toJsonArray(List<String> list) {
        try {
            return objectMapper.writeValueAsString(safeList(list));
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> fromJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
