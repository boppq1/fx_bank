package com.example.bank.fx.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FxDataDao {
	
	void insertRate(
			@Param("currency_code") String currency_code,
			@Param("buy_rate") Double ttb,
			@Param("sell_rate") Double tts,
			@Param("base_rate") Double deal_bas_r
			);

}
