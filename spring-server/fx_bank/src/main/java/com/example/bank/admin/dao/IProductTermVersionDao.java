package com.example.bank.admin.dao;

import com.example.bank.admin.dto.ProductTermVersionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IProductTermVersionDao {

    /** 현재 시행중(is_current='Y') 버전 조회 */
    ProductTermVersionDto selectCurrentVersion(@Param("termsNo") long termsNo);

    /**
     * 특정 시점(asOfDate)에 유효했던 버전 조회 (가입 당시 적용 약관 추적용)
     * effective_dt <= asOfDate AND (expired_dt IS NULL OR expired_dt > asOfDate)
     */
    ProductTermVersionDto selectVersionAt(@Param("termsNo") long termsNo, @Param("asOfDate") String asOfDate);

    /** 특정 버전 단건 조회 */
    ProductTermVersionDto selectVersionByNo(@Param("termVersionNo") long termVersionNo);

    /** 전체 버전 이력 (최신순) */
    List<ProductTermVersionDto> selectVersionHistory(@Param("termsNo") long termsNo);

    /** 가장 최근 버전(번호 기준 max)의 major/minor 값을 가져오기 위한 조회 */
    ProductTermVersionDto selectLatestVersionRow(@Param("termsNo") long termsNo);

    /** 기존 현재버전을 N + expired_dt(now) 처리 */
    int expireCurrentVersion(@Param("termsNo") long termsNo);

    int insertVersion(ProductTermVersionDto dto);
}