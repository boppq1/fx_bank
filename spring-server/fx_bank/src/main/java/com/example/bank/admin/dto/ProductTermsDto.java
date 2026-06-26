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
public class ProductTermsDto {

    private Long termsNo;
    private Long productNo;
    private Long typeNo;
    private String typeName;      // 조회 시 terms_types 조인 결과
    private String requiredYn;
    private String useYn;
    private String createdDt;

    // 조회 편의를 위한 현재 버전 정보 (목록 화면에서 같이 보여주기 위함)
    private ProductTermVersionDto latestVersion;
}