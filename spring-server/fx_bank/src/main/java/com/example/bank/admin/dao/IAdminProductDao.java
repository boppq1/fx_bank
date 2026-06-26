package com.example.bank.admin.dao;

import com.example.bank.admin.dto.ProductDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAdminProductDao {

    List<ProductDto> selectProductListForTerms(@Param("productType") String productType, @Param("keyword") String keyword);
    ProductDto selectProductDetail(@Param("productNo") int productNo);

}