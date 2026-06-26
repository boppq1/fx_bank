package com.example.bank.admin.dao;

import com.example.bank.admin.dto.ProductTermsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IProductTermsDao {

    List<ProductTermsDto> selectTermsListByProduct(@Param("productNo") long productNo);
    ProductTermsDto selectSlotByProductAndType(@Param("productNo") long productNo, @Param("typeNo") long typeNo);
    ProductTermsDto selectSlotByTermsNo(@Param("termsNo") long termsNo);
    int insertSlot(ProductTermsDto dto);
}