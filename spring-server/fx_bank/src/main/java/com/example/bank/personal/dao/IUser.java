package com.example.bank.personal.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.bank.personal.dto.RegisterRequestDto;
import com.example.bank.personal.dto.UserEntity;
@Mapper
public interface IUser {
	//로그인시 사용하는 메서드
	UserEntity findByUserId(@Param("userId")String userId);
	//회원가입시 사용하는 메서드
	int insertUser(RegisterRequestDto registerRequest);
}
