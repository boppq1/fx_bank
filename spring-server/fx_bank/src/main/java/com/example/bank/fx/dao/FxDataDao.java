package com.example.bank.fx.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FxDataDao {
	
	void insertRate(
			@Param("cur_nm") String cur_nm,
			@Param("ttb") Double ttb,
			@Param("tts") Double tts,
			@Param("deal_bas_r") Double deal_bas_r
			);

}
