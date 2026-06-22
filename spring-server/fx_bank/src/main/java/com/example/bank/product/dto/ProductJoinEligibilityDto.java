package com.example.bank.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductJoinEligibilityDto {
    private boolean canJoin;
    private String reason;
}
