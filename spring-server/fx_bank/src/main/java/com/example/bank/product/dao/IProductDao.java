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

    // 외화 상품 목록 (기존: 전체)
    List<ProductListDto> selectForeignProductList();

    // 외화 상품 목록 (검색 + 카테고리 + 페이징)
    List<ProductListDto> selectForeignProductPage(
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("offset") int offset,
            @Param("size") int size
    );

    // 외화 상품 목록 전체 건수 (검색 + 카테고리)
    long countForeignProduct(
            @Param("keyword") String keyword,
            @Param("type") String type
    );

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

    Long selectNextProductReviewNo();

    int countActiveProductSubscription(@Param("userNo") Long userNo, @Param("productNo") Long productNo);

    int countMyProductReview(@Param("userNo") Long userNo, @Param("productNo") Long productNo);

    int insertProductReview(
            @Param("reviewNo") Long reviewNo,
            @Param("userNo") Long userNo,
            @Param("productNo") Long productNo,
            @Param("request") com.example.bank.product.dto.ProductReviewWriteRequestDto request
    );
}
