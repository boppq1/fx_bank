package com.example.bank.product.service;

import java.util.List;

import com.example.bank.product.dto.ProductCurrencyDto;
import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.dto.ProductListDto;
import com.example.bank.product.dto.ProductListPageDto;
import com.example.bank.product.dto.ProductPreferentialRateDto;
import com.example.bank.product.dto.ProductRateDto;
import com.example.bank.product.dto.ProductReviewDto;
import com.example.bank.product.dto.ProductTermDto;

public interface ProductService {

    List<ProductListDto> getForeignProductList();

    // 검색 + 카테고리 + 페이징 목록
    ProductListPageDto getForeignProductPage(String keyword, String type, int page, int size);

    ProductDetailDto getProductDetail(Long productNo);

    List<ProductCurrencyDto> getProductCurrencies(Long productNo);

    List<ProductRateDto> getProductRates(Long productNo);

    List<ProductPreferentialRateDto> getProductPreferentialRates(Long productNo);

    List<ProductTermDto> getProductTerms(Long productNo);

    List<ProductReviewDto> getProductReviews(Long productNo);

    boolean canWriteProductReview(Long userNo, Long productNo);

    Long writeProductReview(
            Long userNo,
            Long productNo,
            com.example.bank.product.dto.ProductReviewWriteRequestDto request
    );
}
