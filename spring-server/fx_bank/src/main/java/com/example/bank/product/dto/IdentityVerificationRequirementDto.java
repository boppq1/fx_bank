package com.example.bank.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IdentityVerificationRequirementDto {
    private boolean required;
    private String level;
    private String reason;
}
