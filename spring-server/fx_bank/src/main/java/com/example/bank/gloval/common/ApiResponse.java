/**
 * ========================================================================
 * 파일명: ApiResponse.java
 * 작성 위치: com.example.global.common
 * 역할: 모든 API 요청에 대한 공통 표준 JSON 응답 포맷 정의
 * ========================================================================
 */
package com.example.bank.gloval.common;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success; // 성공 여부 (true/false)
    private final String message;  // 프론트에 보여줄 메시지
    private final T data;          // 실제 뱉어줄 데이터 (토큰 등)

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // 성공 응답용 팩토리 메서드
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    // 실패 응답용 팩토리 메서드
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
