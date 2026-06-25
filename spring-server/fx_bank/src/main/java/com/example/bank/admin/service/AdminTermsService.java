package com.example.bank.admin.service;

import com.example.bank.admin.dao.IAdminProductDao;
import com.example.bank.admin.dao.IProductTermVersionDao;
import com.example.bank.admin.dao.IProductTermsDao;
import com.example.bank.admin.dto.ProductDto;
import com.example.bank.admin.dto.ProductTermVersionDto;
import com.example.bank.admin.dto.ProductTermsDto;
import com.example.bank.admin.etc.PdfTermsFileHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AdminTermsService {

    private final IAdminProductDao productDao;
    private final IProductTermsDao productTermsDao;
    private final IProductTermVersionDao termVersionDao;
    private final PdfTermsFileHandler pdfTermsFileHandler;

    public List<ProductDto> getProductListForTerms(String productType, String keyword) {
        return productDao.selectProductListForTerms(productType, keyword);
    }

    public ProductDto getProductDetail(int productNo) {
        return productDao.selectProductDetail(productNo);
    }

    public List<ProductTermsDto> getTermsListForProduct(long productNo) {
        return productTermsDao.selectTermsListByProduct(productNo);
    }

    public ProductTermVersionDto getCurrentVersion(long termsNo) {
        return termVersionDao.selectCurrentVersion(termsNo);
    }

    public List<ProductTermVersionDto> getVersionHistory(long termsNo) {
        return termVersionDao.selectVersionHistory(termsNo);
    }

    public ProductTermVersionDto getVersionAt(long termsNo, String asOfDate) {
        return termVersionDao.selectVersionAt(termsNo, asOfDate);
    }

    public ProductTermVersionDto getVersionByNo(long termVersionNo) {
        return termVersionDao.selectVersionByNo(termVersionNo);
    }


    @Transactional
    public long registerTermsVersion(Long termsNo, Long productNo, Long typeNo, String requiredYn,
                                     String termsTitle, String changeReason, MultipartFile pdfFile) {

        long resolvedTermsNo;
        int nextMajor;
        int nextMinor;

        if (termsNo != null) {
            // 기존 슬롯에 새 버전 추가
            ProductTermsDto slot = productTermsDao.selectSlotByTermsNo(termsNo);
            if (slot == null) {
                throw new IllegalArgumentException("존재하지 않는 약관 슬롯입니다. termsNo=" + termsNo);
            }
            resolvedTermsNo = termsNo;

            ProductTermVersionDto latest = termVersionDao.selectLatestVersionRow(resolvedTermsNo);
            if (latest != null) {
                nextMajor = latest.getMajorVersion();
                nextMinor = latest.getMinorVersion() + 1; // 본문 변경 = minor 버전 증가
            } else {
                nextMajor = 1;
                nextMinor = 0;
            }

            // 기존 현재버전 만료 처리
            termVersionDao.expireCurrentVersion(resolvedTermsNo);

        } else {
            // 신규 슬롯 생성
            if (productNo == null || typeNo == null) {
                throw new IllegalArgumentException("신규 등록 시 productNo, typeNo는 필수입니다.");
            }

            ProductTermsDto existingSlot = productTermsDao.selectSlotByProductAndType(productNo, typeNo);
            if (existingSlot != null) {
                throw new IllegalStateException(
                        "이미 해당 상품에 같은 약관 종류가 등록되어 있습니다. termsNo=" + existingSlot.getTermsNo()
                                + " (새 버전을 등록하려면 termsNo를 지정해주세요.)");
            }

            ProductTermsDto newSlot = ProductTermsDto.builder()
                    .productNo(productNo)
                    .typeNo(typeNo)
                    .requiredYn(requiredYn != null ? requiredYn : "Y")
                    .useYn("Y")
                    .build();
            productTermsDao.insertSlot(newSlot);
            resolvedTermsNo = newSlot.getTermsNo();

            nextMajor = 1;
            nextMinor = 0;
        }

        // PDF 저장 + 텍스트 추출
        PdfTermsFileHandler.PdfSaveResult result = pdfTermsFileHandler.saveAndExtract(
                "TERMS-" + resolvedTermsNo, pdfFile);

        Long currentAdminId = getCurrentAdminId();

        ProductTermVersionDto version = ProductTermVersionDto.builder()
                .termsNo(resolvedTermsNo)
                .majorVersion(nextMajor)
                .minorVersion(nextMinor)
                .termsTitle(termsTitle)
                .pdfPath(result.pdfPath())
                .termsText(result.extractedText())
                .changeReason(changeReason)
                .createdBy(currentAdminId)
                .build();

        termVersionDao.insertVersion(version);

        return resolvedTermsNo;
    }


    private Long getCurrentAdminId() {
        try {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            return Long.parseLong(name);
        } catch (Exception e) {
            return null;
        }
    }
}