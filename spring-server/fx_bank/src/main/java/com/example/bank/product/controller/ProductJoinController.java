package com.example.bank.product.controller;

import com.example.bank.gloval.common.ApiResponse;
import com.example.bank.personal.dto.UserEntity;
import com.example.bank.personal.service.OcrService;
import com.example.bank.personal.service.SolapiSmsService;
import com.example.bank.product.dto.ProductJoinCompleteDto;
import com.example.bank.product.dto.CouponDto;
import com.example.bank.product.dto.CouponSelectionRequestDto;
import com.example.bank.product.dto.ProductJoinEligibilityDto;
import com.example.bank.product.dto.IdentityVerificationRequirementDto;
import com.example.bank.product.dto.ProductJoinFormRequestDto;
import com.example.bank.product.dto.ProductJoinResumeDto;
import com.example.bank.product.dto.ProductJoinSubmitRequestDto;
import com.example.bank.product.dto.ProductJoinTermsRequestDto;
import com.example.bank.product.dto.PhoneVerificationRequestDto;
import com.example.bank.product.dto.ProductMySubscriptionDto;
import com.example.bank.product.dto.ProductTermDto;
import com.example.bank.product.dto.WithdrawableForeignAccountDto;
import com.example.bank.product.service.ProductJoinService;
import com.example.bank.util.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

@RestController
@RequestMapping("/api/product/join")
@RequiredArgsConstructor
public class ProductJoinController {

    private final ProductJoinService productJoinService;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final OcrService ocrService;
    private final SolapiSmsService solapiSmsService;

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
            Authentication authentication,
            HttpSession session
    ) {
        try {
            productJoinService.saveTermsToSession(dto, getUserNoFromRedis(authentication), session);
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
            Authentication authentication,
            HttpSession session
    ) {
        try {
            productJoinService.saveJoinFormToSession(dto, getUserNoFromRedis(authentication), session);
            return ApiResponse.success("가입 정보 저장 성공", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{productNo}/coupons")
    public ApiResponse<List<CouponDto>> getAvailableCoupons(
            @PathVariable("productNo") Long productNo,
            Authentication authentication
    ) {
        try {
            Long userNo = getUserNoFromRedis(authentication);
            return ApiResponse.success("사용 가능한 우대금리 쿠폰 조회 성공",
                    productJoinService.getAvailableCoupons(userNo, productNo));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/coupon")
    public ApiResponse<Void> saveCoupon(
            @RequestBody CouponSelectionRequestDto dto,
            Authentication authentication,
            HttpSession session
    ) {
        try {
            productJoinService.saveCouponToSession(dto, getUserNoFromRedis(authentication), session);
            return ApiResponse.success("우대금리 쿠폰 선택이 저장되었습니다.", null);
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

    @GetMapping("/{productNo}/join-eligibility")
    public ApiResponse<ProductJoinEligibilityDto> getJoinEligibility(
            @PathVariable("productNo") Long productNo,
            Authentication authentication
    ) {
        try {
            Long userNo = getUserNoFromRedis(authentication);
            return ApiResponse.success("가입 가능 여부 조회 성공", productJoinService.getJoinEligibility(productNo, userNo));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{productNo}/identity-verification-requirement")
    public ApiResponse<IdentityVerificationRequirementDto> getIdentityVerificationRequirement(
            @PathVariable("productNo") Long productNo,
            Authentication authentication
    ) {
        try {
            Long userNo = getUserNoFromRedis(authentication);
            return ApiResponse.success("본인확인 대상 조회 성공",
                    productJoinService.getIdentityVerificationRequirement(productNo, userNo));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/withdrawable-accounts")
    public ApiResponse<List<WithdrawableForeignAccountDto>> getWithdrawableAccounts(
            @RequestParam("currencyCode") String currencyCode,
            Authentication authentication
    ) {
        try {
            return ApiResponse.success("출금 가능 계좌 조회 성공",
                    productJoinService.getWithdrawableForeignAccounts(getUserNoFromRedis(authentication), currencyCode));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{productNo}/phone-verification/send")
    public ApiResponse<Void> sendPhoneVerification(
            @PathVariable("productNo") Long productNo,
            Authentication authentication
    ) {
        try {
            UserEntity user = getAuthenticatedUserFromRedis(authentication);
            if (productJoinService.getIdentityVerificationRequirement(productNo, user.getUserNo()).isRequired()) {
                return ApiResponse.error("이 상품은 휴대폰 인증 대신 신분증 OCR 인증 대상입니다.");
            }
            solapiSmsService.sendVerificationCode(user.getPhone());
            return ApiResponse.success("인증번호를 발송했습니다.", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{productNo}/phone-verification/confirm")
    public ApiResponse<Void> confirmPhoneVerification(
            @PathVariable("productNo") Long productNo,
            @RequestBody PhoneVerificationRequestDto request,
            Authentication authentication,
            HttpSession session
    ) {
        try {
            UserEntity user = getAuthenticatedUserFromRedis(authentication);
            if (productJoinService.getIdentityVerificationRequirement(productNo, user.getUserNo()).isRequired()) {
                return ApiResponse.error("이 상품은 휴대폰 인증 대신 신분증 OCR 인증 대상입니다.");
            }
            solapiSmsService.verifyCode(user.getPhone(), request.getCode());
            session.setAttribute("PRODUCT_JOIN_PHONE_VERIFIED_PRODUCT_NO", productNo);
            return ApiResponse.success("휴대폰 본인인증이 완료되었습니다.", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping(value = "/{productNo}/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> verifyIdCard(
            @PathVariable("productNo") Long productNo,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            HttpSession session
    ) {
        try {
            UserEntity user = getAuthenticatedUserFromRedis(authentication);
            Map<String, Object> ocrResult = ocrService.recognizeIdCard(file);

            boolean ocrSuccess = Boolean.parseBoolean(String.valueOf(ocrResult.get("success")));
            String ocrName = String.valueOf(ocrResult.getOrDefault("name", ""));
            String rrnMasked = String.valueOf(ocrResult.getOrDefault("rrnMasked", ""));
            boolean nameMatched = normalize(ocrName).equals(normalize(user.getNameKo()));
            boolean birthMatched = user.getBirthDate() != null
                    && rrnMasked.startsWith(new SimpleDateFormat("yyMMdd").format(user.getBirthDate()));

            Long verificationNo = productJoinService.saveOcrVerification(
                    productNo, user.getUserNo(), ocrSuccess, nameMatched, birthMatched, session
            );
            return ApiResponse.success("신분증 OCR 인증이 완료되었습니다.", verificationNo);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping(value = "/{productNo}/terms/{termsCode}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> getTermsPdf(
            @PathVariable("productNo") Long productNo,
            @PathVariable("termsCode") String termsCode
    ) {
        ProductTermDto term = productJoinService.getJoinTerms(productNo).stream()
                .filter(item -> termsCode.equals(item.getTermsCode()))
                .findFirst()
                .orElse(null);
        if (term == null || term.getPdfPath() == null || term.getPdfPath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Path pdfPath = Path.of(term.getPdfPath()).normalize();
        if (!Files.isRegularFile(pdfPath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(pdfPath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=terms.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping(value = "/{productNo}/terms/{termsCode}/pdf/page/{pageNo}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getTermsPdfPageImage(
            @PathVariable("productNo") Long productNo,
            @PathVariable("termsCode") String termsCode,
            @PathVariable("pageNo") int pageNo
    ) {
        ProductTermDto term = productJoinService.getJoinTerms(productNo).stream()
                .filter(item -> termsCode.equals(item.getTermsCode()))
                .findFirst()
                .orElse(null);
        if (term == null || term.getPdfPath() == null || term.getPdfPath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Path pdfPath = Path.of(term.getPdfPath()).normalize();
        if (!Files.isRegularFile(pdfPath) || pageNo < 1) {
            return ResponseEntity.notFound().build();
        }

        try (PDDocument document = PDDocument.load(pdfPath.toFile());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (pageNo > document.getNumberOfPages()) {
                return ResponseEntity.notFound().build();
            }

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageNo - 1, 135, ImageType.RGB);
            ImageIO.write(image, "jpg", output);

            return ResponseEntity.ok()
                    .cacheControl(org.springframework.http.CacheControl.noCache())
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(output.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/my-subscriptions")
    public ApiResponse<List<ProductMySubscriptionDto>> getMySubscriptions(Authentication authentication) {
        try {
            Long userNo = getUserNoFromRedis(authentication);
            return ApiResponse.success("내 가입 상품 조회 성공", productJoinService.getMySubscriptions(userNo));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 임시저장 / 이어서 가입 =====

    /** 약관 페이지 진입 시 호출: 재개 가능 여부 + 모달/프리필 요약 */
    @GetMapping("/{productNo}/resume")
    public ApiResponse<ProductJoinResumeDto> getResume(
            @PathVariable("productNo") Long productNo,
            Authentication authentication
    ) {
        try {
            Long userNo = getUserNoFromRedis(authentication);
            return ApiResponse.success("이어서 가입 정보 조회 성공", productJoinService.getResumeInfo(userNo, productNo));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /** "이어서 하기": 세션 복원 후 라우팅/프리필 반환 */
    @PostMapping("/{productNo}/resume")
    public ApiResponse<ProductJoinResumeDto> resume(
            @PathVariable("productNo") Long productNo,
            Authentication authentication,
            HttpSession session
    ) {
        try {
            Long userNo = getUserNoFromRedis(authentication);
            return ApiResponse.success("이어서 가입을 시작합니다.",
                    productJoinService.resumeIntoSession(userNo, productNo, session));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /** "새로 시작": 기존 진행행 만료 처리 */
    @PostMapping("/{productNo}/resume/discard")
    public ApiResponse<Void> discardResume(
            @PathVariable("productNo") Long productNo,
            Authentication authentication
    ) {
        try {
            Long userNo = getUserNoFromRedis(authentication);
            productJoinService.discardProgress(userNo, productNo);
            return ApiResponse.success("새로 시작합니다.", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private UserEntity getAuthenticatedUserFromRedis(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String userId = authentication.getPrincipal().toString();
        if (userId.isBlank() || "anonymousUser".equals(userId)) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String userJson = redisUtil.getData("USER:" + userId);
        if (userJson == null || userJson.isBlank()) {
            throw new IllegalArgumentException("로그인 정보가 만료되었습니다. 다시 로그인해주세요.");
        }

        try {
            UserEntity user = objectMapper.readValue(userJson, UserEntity.class);
            if (user.getUserNo() == null) {
                throw new IllegalArgumentException("Redis 사용자 정보에 userNo가 없습니다.");
            }
            return user;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Redis 사용자 정보를 읽을 수 없습니다.", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
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
