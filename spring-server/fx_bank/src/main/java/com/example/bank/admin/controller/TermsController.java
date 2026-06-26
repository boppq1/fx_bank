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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/admin/terms")
@RequiredArgsConstructor
public class TermsController {

    private final AdminTermsService termsServ;

    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getProductList(
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(termsServ.getProductListForTerms(productType, keyword));
    }

    @GetMapping("/products/{productNo}")
    public ResponseEntity<ProductDto> getProductDetail(@PathVariable int productNo) {
        return ResponseEntity.ok(termsServ.getProductDetail(productNo));
    }

    @GetMapping("/products/{productNo}/terms")
    public ResponseEntity<List<ProductTermsDto>> getTermsList(@PathVariable long productNo) {
        return ResponseEntity.ok(termsServ.getTermsListForProduct(productNo));
    }

    @GetMapping("/{termsNo}/current")
    public ResponseEntity<ProductTermVersionDto> getCurrentVersion(@PathVariable long termsNo) {
        return ResponseEntity.ok(termsServ.getCurrentVersion(termsNo));
    }

    @GetMapping("/{termsNo}/versions")
    public ResponseEntity<List<ProductTermVersionDto>> getVersionHistory(@PathVariable long termsNo) {
        return ResponseEntity.ok(termsServ.getVersionHistory(termsNo));
    }

    @GetMapping("/versions/{termVersionNo}")
    public ResponseEntity<ProductTermVersionDto> getVersionDetail(@PathVariable long termVersionNo) {
        return ResponseEntity.ok(termsServ.getVersionByNo(termVersionNo));
    }

    @GetMapping("/{termsNo}/version-at")
    public ResponseEntity<ProductTermVersionDto> getVersionAt(
            @PathVariable long termsNo,
            @RequestParam String date) {
        return ResponseEntity.ok(termsServ.getVersionAt(termsNo, date));
    }

   
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

    @GetMapping("/versions/{termVersionNo}/pdf")
    public ResponseEntity<Resource> downloadPdf(@PathVariable long termVersionNo) {
        ProductTermVersionDto version = termsServ.getVersionByNo(termVersionNo);
        if (version == null || version.getPdfPath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(version.getPdfPath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String fileName = filePath.getFileName().toString();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
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