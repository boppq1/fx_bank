package com.example.bank.admin.dao;

import com.example.bank.admin.dto.ProductTermVersionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IProductTermVersionDao {

    ProductTermVersionDto selectCurrentVersion(@Param("termsNo") long termsNo);

    ProductTermVersionDto selectVersionAt(@Param("termsNo") long termsNo, @Param("asOfDate") String asOfDate);

    ProductTermVersionDto selectVersionByNo(@Param("termVersionNo") long termVersionNo);

    List<ProductTermVersionDto> selectVersionHistory(@Param("termsNo") long termsNo);

    ProductTermVersionDto selectLatestVersionRow(@Param("termsNo") long termsNo);

    int expireCurrentVersion(@Param("termsNo") long termsNo);

    int insertVersion(ProductTermVersionDto dto);
}