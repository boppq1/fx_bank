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

/**
 * 약관 등록/조회 서비스 (버전 추적 중심, 승인 워크플로우 없음)
 *
 * 데이터 구조:
 *  - product_terms      : 상품 x 약관종류 슬롯 (1개씩만 존재, UNIQUE(product_no, type_no))
 *  - product_term_versions : 슬롯별 버전들. is_current='Y' 가 현재 시행 버전.
 *
 * 새 PDF를 등록하면:
 *  1) 슬롯이 없으면 새로 생성 (최초 등록)
 *  2) 슬롯의 기존 현재버전이 있으면 만료 처리(is_current='N', expired_dt=now)
 *  3) PDF 저장 + 텍스트 추출 후 새 버전을 is_current='Y'로 insert (버전 번호는 minor +1 증가)
 */
@Service
@RequiredArgsConstructor
public class AdminTermsService {

    private final IAdminProductDao productDao;
    private final IProductTermsDao productTermsDao;
    private final IProductTermVersionDao termVersionDao;
    private final PdfTermsFileHandler pdfTermsFileHandler;

    /* ===================== 기획 상품 조회 ===================== */
    public List<ProductDto> getProductListForTerms(String productType, String keyword) {
        return productDao.selectProductListForTerms(productType, keyword);
    }

    public ProductDto getProductDetail(int productNo) {
        return productDao.selectProductDetail(productNo);
    }

    /* ===================== 약관 슬롯 / 버전 조회 ===================== */

    /** 상품에 연결된 약관 슬롯 목록 (각 슬롯의 현재 버전 정보 포함) */
    public List<ProductTermsDto> getTermsListForProduct(long productNo) {
        return productTermsDao.selectTermsListByProduct(productNo);
    }

    /** 특정 슬롯의 현재(시행중) 버전 상세 (본문 포함) */
    public ProductTermVersionDto getCurrentVersion(long termsNo) {
        return termVersionDao.selectCurrentVersion(termsNo);
    }

    /** 특정 슬롯의 전체 버전 이력 (최신순, 본문 제외) */
    public List<ProductTermVersionDto> getVersionHistory(long termsNo) {
        return termVersionDao.selectVersionHistory(termsNo);
    }

    /**
     * 특정 시점에 유효했던 버전 조회 (가입 당시 적용 약관 추적용)
     * @param asOfDate 'YYYY-MM-DD' 형식
     */
    public ProductTermVersionDto getVersionAt(long termsNo, String asOfDate) {
        return termVersionDao.selectVersionAt(termsNo, asOfDate);
    }

    public ProductTermVersionDto getVersionByNo(long termVersionNo) {
        return termVersionDao.selectVersionByNo(termVersionNo);
    }

    /* ===================== 약관 PDF 등록 (신규 슬롯 또는 새 버전) ===================== */

    /**
     * PDF 약관 등록
     * - termsNo가 없으면(신규): product_no+type_no로 슬롯 신규 생성 후 1.0 버전 등록
     * - termsNo가 있으면(버전 추가): 기존 슬롯의 현재버전을 만료시키고 새 버전(minor+1) 등록
     *
     * @param termsNo      기존 슬롯 번호 (null이면 신규 슬롯 생성)
     * @param productNo    상품 번호 (신규 슬롯 생성 시 필요)
     * @param typeNo       약관 종류 번호 (신규 슬롯 생성 시 필요)
     * @param requiredYn   필수 가입 여부 (신규 슬롯 생성 시 필요)
     * @param termsTitle   약관명
     * @param changeReason 변경/등록 사유
     * @param pdfFile      업로드된 PDF
     * @return 등록된 약관 슬롯 번호(termsNo)
     */
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

    /* ===================== 내부 유틸 ===================== */

    /**
     * 로그인된 관리자의 admin_no.
     * Authentication.getName()이 admin_no(숫자) 그대로 들어있다는 전제.
     * 인증 정보가 없거나 숫자로 파싱할 수 없으면 null 반환(저장은 계속 진행).
     */
    private Long getCurrentAdminId() {
        try {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            return Long.parseLong(name);
        } catch (Exception e) {
            return null;
        }
    }
}