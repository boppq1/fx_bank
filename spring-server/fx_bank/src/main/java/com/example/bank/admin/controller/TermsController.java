package com.example.bank.admin.controller;

import com.example.bank.admin.dto.ProductDto;
import com.example.bank.admin.dto.ProductTermVersionDto;
import com.example.bank.admin.dto.ProductTermsDto;
import com.example.bank.admin.service.AdminTermsService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * [약관팀 화면]
 * - 상품기획팀이 등록한 상품 조회
 * - 약관 PDF 등록 (신규 슬롯 생성 또는 기존 슬롯에 새 버전 추가)
 * - 약관 버전 이력 / 특정 시점 버전 조회
 */
@RestController
@RequestMapping("/admin/terms")
@RequiredArgsConstructor
public class TermsController {

    private final AdminTermsService termsServ;

    /** 약관 등록 대상 상품 목록 조회 */
    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getProductList(
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(termsServ.getProductListForTerms(productType, keyword));
    }

    /** 상품 상세 조회 */
    @GetMapping("/products/{productNo}")
    public ResponseEntity<ProductDto> getProductDetail(@PathVariable int productNo) {
        return ResponseEntity.ok(termsServ.getProductDetail(productNo));
    }

    /** 특정 상품의 약관 슬롯 목록 (슬롯별 현재 버전 정보 포함) */
    @GetMapping("/products/{productNo}/terms")
    public ResponseEntity<List<ProductTermsDto>> getTermsList(@PathVariable long productNo) {
        return ResponseEntity.ok(termsServ.getTermsListForProduct(productNo));
    }

    /** 약관 슬롯의 현재(시행중) 버전 상세 (본문 포함) */
    @GetMapping("/{termsNo}/current")
    public ResponseEntity<ProductTermVersionDto> getCurrentVersion(@PathVariable long termsNo) {
        return ResponseEntity.ok(termsServ.getCurrentVersion(termsNo));
    }

    /** 약관 슬롯의 전체 버전 이력 (최신순) */
    @GetMapping("/{termsNo}/versions")
    public ResponseEntity<List<ProductTermVersionDto>> getVersionHistory(@PathVariable long termsNo) {
        return ResponseEntity.ok(termsServ.getVersionHistory(termsNo));
    }

    /** 특정 버전 단건 상세 */
    @GetMapping("/versions/{termVersionNo}")
    public ResponseEntity<ProductTermVersionDto> getVersionDetail(@PathVariable long termVersionNo) {
        return ResponseEntity.ok(termsServ.getVersionByNo(termVersionNo));
    }

    /**
     * 특정 시점(가입 당시 등)에 유효했던 버전 조회
     * 예: GET /admin/terms/{termsNo}/version-at?date=2025-03-15
     */
    @GetMapping("/{termsNo}/version-at")
    public ResponseEntity<ProductTermVersionDto> getVersionAt(
            @PathVariable long termsNo,
            @RequestParam String date) {
        return ResponseEntity.ok(termsServ.getVersionAt(termsNo, date));
    }

    /**
     * 약관 PDF 등록
     * - termsNo가 없으면 신규 슬롯 생성 + 1.0 버전 등록 (이때 productNo, typeNo, requiredYn 필요)
     * - termsNo가 있으면 기존 슬롯에 새 버전(minor+1) 추가, 기존 현재버전은 만료 처리
     *
     * Postman 테스트 (form-data):
     *  - key "terms" -> Content-Type: application/json
     *      신규: {"productNo": 1, "typeNo": 1, "requiredYn": "Y", "termsTitle": "예금거래기본약관", "changeReason": "최초 등록"}
     *      버전추가: {"termsNo": 5, "termsTitle": "예금거래기본약관", "changeReason": "이자율 조항 변경"}
     *  - key "file" -> PDF 파일
     */
    @PostMapping(value = "/register/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> registerTermsVersion(
            @RequestPart("terms") TermsRegisterRequest req,
            @RequestPart("file") MultipartFile file) {

        long termsNo = termsServ.registerTermsVersion(
                req.getTermsNo(), req.getProductNo(), req.getTypeNo(), req.getRequiredYn(),
                req.getTermsTitle(), req.getChangeReason(), file);

        return ResponseEntity.ok(Map.of(
                "termsNo", termsNo,
                "message", "약관 버전이 등록되었습니다."
        ));
    }

    @Getter
    @Setter
    public static class TermsRegisterRequest {
        private Long termsNo;     // 있으면 기존 슬롯에 새 버전 추가
        private Long productNo;   // 신규 슬롯 생성 시 필요
        private Long typeNo;      // 신규 슬롯 생성 시 필요
        private String requiredYn; // 신규 슬롯 생성 시 필요 (Y/N)
        private String termsTitle;
        private String changeReason;
    }
}