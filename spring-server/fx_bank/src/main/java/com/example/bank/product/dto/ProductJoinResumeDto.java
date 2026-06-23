package com.example.bank.product.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * 이어서 가입(임시저장 복원) 응답 DTO.
 * - GET  /api/product/join/{productNo}/resume : 재개 가능 여부 + 모달 표시용 요약
 * - POST /api/product/join/{productNo}/resume : 세션 복원 후 라우팅 + 폼 프리필 데이터
 *
 * 보안/정확성 원칙:
 * - 계좌비밀번호·출금계좌는 영속화하지 않으므로 프리필에 포함하지 않는다(폼에서 재입력).
 * - baseRate 는 rate_no 의 product_rates 기본금리이며, 저장된 applied_rate(쿠폰 우대분 섞였을 수 있음)는 표시 금리로 쓰지 않는다.
 */
@Data
public class ProductJoinResumeDto {

    private boolean available;          // 재개 가능 여부
    private String routeStep;           // "VERIFY" | "COUPON" (D: 인증 무효면 VERIFY 우선)
    private String routeUrl;            // 프론트 네비게이션 편의 URL
    private String currentStep;         // 마지막 완료 단계(progress.current_step)

    private Long productNo;
    private String productName;

    // ===== 폼 프리필(비민감 값만) =====
    private Long rateNo;
    private String currencyCode;
    private BigDecimal amount;
    private Integer periodMonth;
    private BigDecimal baseRate;        // rate_no 기본금리(표시용). 우대분은 쿠폰 재선택 후 재계산

    // ===== 본인인증 상태 =====
    private boolean identityRequired;
    private boolean identityVerified;

    private Date updatedDt;
    private Date expiredDt;

    public static ProductJoinResumeDto notAvailable() {
        ProductJoinResumeDto dto = new ProductJoinResumeDto();
        dto.setAvailable(false);
        return dto;
    }
}
