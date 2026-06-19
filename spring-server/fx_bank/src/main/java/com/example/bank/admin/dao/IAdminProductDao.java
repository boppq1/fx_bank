package com.example.bank.admin.dao;

import com.example.bank.admin.dto.ProductDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAdminProductDao {

    /** 약관팀용 상품 목록 조회 (slot 수/미등록 슬롯 수 집계 포함) */
    List<ProductDto> selectProductListForTerms(@Param("productType") String productType, @Param("keyword") String keyword);

    /** 상품 단건 상세 조회 */
    ProductDto selectProductDetail(@Param("productNo") int productNo);

}