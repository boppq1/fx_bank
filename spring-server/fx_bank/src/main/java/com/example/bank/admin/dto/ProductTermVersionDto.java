package com.example.bank.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


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