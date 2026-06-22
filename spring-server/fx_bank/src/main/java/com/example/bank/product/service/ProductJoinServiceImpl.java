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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.product.dao.IProductJoinDao;
import com.example.bank.product.dto.ElectronicSignatureDto;
import com.example.bank.product.dto.ForeignAccountBalanceInsertDto;
import com.example.bank.product.dto.ForeignAccountInsertDto;
import com.example.bank.product.dto.IdVerificationDto;
import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.dto.ProductJoinCompleteDto;
import com.example.bank.product.dto.ProductJoinFormRequestDto;
import com.example.bank.product.dto.ProductJoinSubmitRequestDto;
import com.example.bank.product.dto.ProductJoinTermsRequestDto;
import com.example.bank.product.dto.ProductSubscriptionInsertDto;
import com.example.bank.product.dto.ProductTermDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductJoinServiceImpl implements ProductJoinService {

    private final IProductJoinDao productJoinDao;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.file.signature-dir:uploads/signatures}")
    private String signatureDir;

    private static final String SESSION_TERMS = "PRODUCT_JOIN_TERMS";
    private static final String SESSION_FORM = "PRODUCT_JOIN_FORM";
    private static final String SESSION_VERIFICATION_NO = "PRODUCT_JOIN_VERIFICATION_NO";
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

    // =====================================================
    // 2. 약관 동의 세션 저장
    // =====================================================

    @Override
    public void saveTermsToSession(ProductJoinTermsRequestDto dto, HttpSession session) {
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

        return verificationNo;
    }

    // =====================================================
    // 4. 가입 정보 세션 저장
    // =====================================================

    @Override
    public void saveJoinFormToSession(ProductJoinFormRequestDto dto, HttpSession session) {
        if (dto == null || dto.getProductNo() == null) {
            throw new IllegalArgumentException("가입 정보가 없습니다.");
        }

        if (dto.getRateNo() == null) {
            throw new IllegalArgumentException("금리 정보가 없습니다.");
        }

        if (dto.getCurrencyCode() == null || dto.getCurrencyCode().isBlank()) {
            throw new IllegalArgumentException("통화 정보가 없습니다.");
        }

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("가입 금액을 확인해주세요.");
        }

        if (dto.getPeriodMonth() == null || dto.getPeriodMonth() <= 0) {
            throw new IllegalArgumentException("가입 기간을 확인해주세요.");
        }

        if (dto.getAppliedRate() == null) {
            throw new IllegalArgumentException("적용 금리가 없습니다.");
        }

        Long productNo = dto.getProductNo();

        ProductDetailDto product = productJoinDao.selectProductForJoin(productNo);
        if (product == null) {
            throw new IllegalArgumentException("가입 가능한 상품이 아닙니다.");
        }

        int currencyCount = productJoinDao.countProductCurrency(productNo, dto.getCurrencyCode());
        if (currencyCount == 0) {
            throw new IllegalArgumentException("해당 상품에서 지원하지 않는 통화입니다.");
        }

        int rateCount = productJoinDao.countProductRate(
                productNo,
                dto.getRateNo(),
                dto.getPeriodMonth()
        );

        if (rateCount == 0) {
            throw new IllegalArgumentException("해당 상품의 금리 정보가 올바르지 않습니다.");
        }

        if (dto.getAccountPassword() == null || dto.getAccountPassword().isBlank()) {
            throw new IllegalArgumentException("계좌 비밀번호를 입력해주세요.");
        }

        session.setAttribute(SESSION_FORM, dto);
    }

    // =====================================================
    // 5. 최종 가입 저장
    // =====================================================

    @Override
    @Transactional
    public Long completeJoin(ProductJoinSubmitRequestDto dto, Long userNo, HttpSession session) {
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

        Long verificationNo =
                (Long) session.getAttribute(SESSION_VERIFICATION_NO);

        if (termsDto == null) {
            throw new IllegalArgumentException("약관 동의 정보가 없습니다.");
        }

        if (formDto == null) {
            throw new IllegalArgumentException("가입 정보가 없습니다.");
        }

        if (verificationNo == null) {
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

        int validVerificationCount = productJoinDao.countValidVerification(
                verificationNo,
                userNo,
                productNo
        );

        if (validVerificationCount == 0) {
            throw new IllegalArgumentException("유효한 OCR 인증 정보가 없습니다.");
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
        subscriptionDto.setType("FOREIGN_DEPOSIT");
        // 상품 가입 정보에는 방금 만든 실제 외화 계좌번호를 연결한다.
        // 현재 DDL에 FK는 없지만, foreign_accounts.account_no와 같은 값을 넣어 업무적으로 연결한다.
        subscriptionDto.setAcntNo(accountNo);
        subscriptionDto.setCurrencyCode(formDto.getCurrencyCode());
        subscriptionDto.setAmount(formDto.getAmount());
        subscriptionDto.setPeriodMonth(formDto.getPeriodMonth());
        subscriptionDto.setVerificationNo(verificationNo);
        subscriptionDto.setSubscriptionStatus("가입완료");
        subscriptionDto.setMaturityDt(calculateMaturityDate(formDto.getPeriodMonth()));
        subscriptionDto.setAppliedRate(formDto.getAppliedRate());
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
        signatureDto.setSignedContent(makeSignedContent(subscriptionNo, productNo, formDto, termsDto));
        signatureDto.setSignedDt(new Date());

        productJoinDao.insertElectronicSignature(signatureDto);

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
        if (product == null || product.getProductType() == null) {
            return false;
        }

        String productType = product.getProductType();
        // "예금"은 정기예금에도 포함되는 단어라 여기 조건에 넣으면 정기예금이 입출금식으로 오분류된다.
        return productType.contains("통장") || productType.contains("입출금"); // 입출금인지 통장인지 판단
    }

    private String saveSignatureImage(Long subscriptionNo, String signatureImageData) {
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
            ProductJoinTermsRequestDto termsDto
    ) {
        Map<String, Object> signedContent = new LinkedHashMap<>();

        signedContent.put("subscriptionNo", subscriptionNo);
        signedContent.put("productNo", productNo);
        signedContent.put("rateNo", formDto.getRateNo());
        signedContent.put("currencyCode", formDto.getCurrencyCode());
        signedContent.put("amount", formDto.getAmount());
        signedContent.put("periodMonth", formDto.getPeriodMonth());
        signedContent.put("appliedRate", formDto.getAppliedRate());
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
    }
}
