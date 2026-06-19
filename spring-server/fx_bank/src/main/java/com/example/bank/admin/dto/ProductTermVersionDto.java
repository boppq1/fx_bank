package com.example.bank.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 약관 버전 (product_term_versions)
 * - 한 행 = 그 시점의 약관 본문 한 버전
 * - is_current='Y' 인 행이 현재 유효한(시행중) 버전
 * - effective_dt ~ expired_dt 로 "어느 기간에 유효했는지" 추적 가능 (가입 당시 버전 조회용)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTermVersionDto {

    private Long termVersionNo;
    private Long termsNo;
    private Integer majorVersion;
    private Integer minorVersion;
    private String termsTitle;
    private String pdfPath;
    private String termsText;
    private String changeReason;
    private String effectiveDt;
    private String expiredDt;
    private String isCurrent;
    private String createdDt;
    private Long createdBy;

    public String getVersionLabel() {
        return majorVersion + "." + minorVersion;
    }
}