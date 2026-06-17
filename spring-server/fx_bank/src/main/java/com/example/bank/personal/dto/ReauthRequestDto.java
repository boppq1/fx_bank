package com.example.bank.personal.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * [사용 위치: OCR 재인증]
 * 보관 만료(3일 경과)로 민감정보가 삭제된 사용자가 신분증을 재촬영하여
 * 주민번호를 다시 등록할 때 사용하는 요청 DTO.
 * 사용자 식별은 인증된 토큰(SecurityContext)에서 추출하므로 userId 는 받지 않는다.
 */
@Getter
@Setter
@ToString
public class ReauthRequestDto {
    @ToString.Exclude
    private String rrn;       // 사용자가 보정한 주민번호 원문(앞6+뒤7)
    private String rrnMasked; // OCR 마스킹 문자열
}
