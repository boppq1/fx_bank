package com.example.bank.product.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

// 상품 리뷰 DTO
@Data
public class ProductReviewDto {

    private Long reviewNo; // 리뷰 번호
    private Long userNo; // 사용자 번호
    private Long productNo; // 어떤 상품의 리뷰인지
    private String userName; // 작성자 이름
    private String reviewText; // 리뷰 내용
    private BigDecimal rating; // 별점
    private Date createdDt; // 작성일
    private Date updatedDt; // 수정일
}