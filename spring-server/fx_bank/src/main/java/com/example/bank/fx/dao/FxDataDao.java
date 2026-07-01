package com.example.bank.fx.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.bank.fx.dto.FxDataDto;

@Mapper
public interface FxDataDao {

    void insertRate(
            @Param("currency_code") String currencyCode,
            @Param("buy_rate") Double buyRate,
            @Param("sell_rate") Double sellRate,
            @Param("base_rate") Double baseRate,
            @Param("announced_at") String announcedAt
    );

    int countRatesByDate(@Param("announced_date") String announcedDate);

    List<FxDataDto> selectLatestRatesByCurrencies(@Param("currencyCodes") List<String> currencyCodes);

    List<FxDataDto> selectAllLatestRates();

    List<FxDataDto> selectAll();

    List<FxDataDto> selectRateHistory(@Param("currencyCode") String currencyCode);

    FxDataDto selectLatestRate(@Param("currencyCode") String currencyCode, @Param("date") String date);
}
