package com.example.bank.product.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class CouponDto {
    private Long couponNo;
    private Long userNo;
    private Long eventPk;
    private Long productNo;
    private BigDecimal preferentialRate;
    private String isUsed;
    private Date issuedDt;
    private Date usedDt;
}
