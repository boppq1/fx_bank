package com.example.bank.product.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

// Login user's product subscription and linked foreign account summary.
@Data
public class ProductMySubscriptionDto {
    private Long subscriptionNo;
    private Long productNo;
    private String productName;
    private String productType;
    private String accountNo;
    private String currencyCode;
    private BigDecimal subscriptionAmount;
    private BigDecimal accountBalance;
    private Date subscribedDt;
    private Date maturityDt;
    private String subscriptionStatus;
}
