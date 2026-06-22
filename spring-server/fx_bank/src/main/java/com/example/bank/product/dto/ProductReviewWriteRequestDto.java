package com.example.bank.product.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductReviewWriteRequestDto {
    private String reviewText;
    private BigDecimal rating;
}
