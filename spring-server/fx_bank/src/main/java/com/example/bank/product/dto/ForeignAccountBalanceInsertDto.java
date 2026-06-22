package com.example.bank.product.dto;

import java.math.BigDecimal;

import lombok.Data;

// foreign_account_balances 테이블에 외화 계좌의 통화별 잔액을 만들 때 사용하는 DTO
@Data
public class ForeignAccountBalanceInsertDto {
    private Long balanceNo;
    private Long fxAcntId;
    private String currencyCode;
    private BigDecimal balance;
}
