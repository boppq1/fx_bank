package com.example.bank.admin.dao;

import com.example.bank.admin.dto.TermsTypeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ITermsTypeDao {

    List<TermsTypeDto> selectTypeList();
    int countByTypeName(@Param("typeName") String typeName);
    int insertType(TermsTypeDto dto);
}