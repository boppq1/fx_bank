package com.example.bank.product.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.bank.product.dto.ProductCurrencyDto;
import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.dto.ProductListDto;
import com.example.bank.product.dto.ProductPreferentialRateDto;
import com.example.bank.product.dto.ProductRateDto;
import com.example.bank.product.dto.ProductReviewDto;
import com.example.bank.product.dto.ProductTermDto;

@Mapper
public interface IProductDao {

    // 외화 상품 목록
    List<ProductListDto> selectForeignProductList();

    // 상품 상세 기본 정보
    ProductDetailDto selectProductDetail(@Param("productNo") Long productNo);

    // 상품별 통화
    List<ProductCurrencyDto> selectProductCurrencies(@Param("productNo") Long productNo);

    // 상품별 금리
    List<ProductRateDto> selectProductRates(@Param("productNo") Long productNo);

    // 상품별 우대금리
    List<ProductPreferentialRateDto> selectProductPreferentialRates(@Param("productNo") Long productNo);

    // 상품별 약관
    List<ProductTermDto> selectProductTerms(@Param("productNo") Long productNo);

    // 상품별 리뷰
    List<ProductReviewDto> selectProductReviews(@Param("productNo") Long productNo);
}