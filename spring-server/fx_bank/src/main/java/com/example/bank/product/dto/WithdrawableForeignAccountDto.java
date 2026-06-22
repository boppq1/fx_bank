package com.example.bank.product.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WithdrawableForeignAccountDto {
    private String accountNo;
    private String currencyCode;
    private BigDecimal balance;
}
