package com.example.bank.product.dao;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.bank.product.dto.ProductJoinProgressDto;

/**
 * 상품 가입 임시저장(product_join_progress) 전용 DAO.
 * user_no + product_no 당 IN_PROGRESS 행 1개 유지(MERGE upsert). 행은 삭제하지 않고 상태로 관리한다.
 */
@Mapper
public interface IProductJoinProgressDao {

    /** MERGE 기반 upsert (user_no + product_no + IN_PROGRESS 1건). 동시성 방어 목적. */
    int upsertProgress(ProductJoinProgressDto dto);

    /** 동시 INSERT 충돌(ORA-00001) 시 폴백용: IN_PROGRESS 행을 스냅샷으로 UPDATE. */
    int updateInProgress(ProductJoinProgressDto dto);

    /** 최신 IN_PROGRESS·미만료 1건 (중복 존재 시 가장 최근 것). */
    ProductJoinProgressDto selectLatestInProgress(
            @Param("userNo") Long userNo,
            @Param("productNo") Long productNo
    );

    /** 방어적 정리: keepNo 를 제외한 IN_PROGRESS 행을 EXPIRED 처리. */
    int expireOtherInProgress(
            @Param("userNo") Long userNo,
            @Param("productNo") Long productNo,
            @Param("keepNo") Long keepNo
    );

    /** 새로 시작/무효 시: 해당 user+product 의 IN_PROGRESS 전부 EXPIRED 처리. */
    int expireAllInProgress(
            @Param("userNo") Long userNo,
            @Param("productNo") Long productNo
    );

    /** 가입 완료 시: IN_PROGRESS 행을 COMPLETED 로 마킹(감사용 보존). */
    int markCompleted(
            @Param("userNo") Long userNo,
            @Param("productNo") Long productNo
    );

    /** 프리필용: rate_no 의 기본금리(product_rates.interest_rate). */
    BigDecimal selectRateInterest(
            @Param("productNo") Long productNo,
            @Param("rateNo") Long rateNo
    );
}
