package com.example.bank.admin.dao;

import com.example.bank.admin.dto.ProductTermsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IProductTermsDao {

    /** 상품의 약관 슬롯 목록 (각 슬롯의 최신 버전 정보를 조인하여 함께 반환) */
    List<ProductTermsDto> selectTermsListByProduct(@Param("productNo") long productNo);

    /** 단건 슬롯 조회 (상품+종류 조합으로 기존 슬롯 존재 여부 확인용) */
    ProductTermsDto selectSlotByProductAndType(@Param("productNo") long productNo, @Param("typeNo") long typeNo);

    /** 슬롯 단건 조회 (termsNo로) */
    ProductTermsDto selectSlotByTermsNo(@Param("termsNo") long termsNo);

    int insertSlot(ProductTermsDto dto);
}