package com.example.bank.product.constant;

/**
 * 상품 가입 임시저장(product_join_progress.current_step)에 들어가는 "가장 최근 완료 단계" 상수.
 * 화면 흐름: 약관(TERMS) → 쿠폰(COUPON) → 본인인증(VERIFY) → 폼(FORM) → 서명/완료(체크포인트 없음).
 * DTO 주석 힌트(FORM/TERMS/VERIFY/SIGN)와 정합. 서명/완료는 최종 확정이라 진행 단계로 두지 않는다.
 */
public enum JoinProgressStep {
    TERMS,
    COUPON,
    VERIFY,
    FORM;

    /** progress_status 값 */
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_EXPIRED = "EXPIRED"; // 만료/새로시작 시 감사용으로 남기는 상태(삭제하지 않음)
}
