package com.example.bank.product.dto;

import java.math.BigDecimal;

import lombok.Data;

// foreign_accounts 테이블에 실제 외화 계좌 1건을 생성할 때 사용하는 DTO
@Data
public class ForeignAccountInsertDto {
    private Long fxAcntNo;
    private String acntPw;
    private String bankName;
    private String accountNo;
    private Long userNo;
    private BigDecimal limitOnce;
    private BigDecimal limitDaily;
}
