package com.example.bank.personal.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.bank.personal.dto.RegisterRequestDto;
import com.example.bank.personal.dto.UserEntity;
import com.example.bank.personal.dto.UserSensitiveInfoEntity;

@Mapper
public interface IUser {
	// 로그인/조회 시 사용 (rrn 미포함)
	UserEntity findByUserId(@Param("userId") String userId);

	// 회원가입: users INSERT (<selectKey> 로 user_no 채번 후 registerRequest.userNo 에 채워짐)
	int insertUser(RegisterRequestDto registerRequest);

	// 로그인 성공 시 last_login_at 갱신
	int updateLastLoginAt(@Param("userNo") Long userNo);

	// ===== user_sensitive_infos (주민번호 전용) =====
	// 회원가입 / 재인증: 민감정보 INSERT
	int insertUserSensitiveInfo(UserSensitiveInfoEntity sensitiveInfo);

	// 만료 체크/조회용
	UserSensitiveInfoEntity findSensitiveByUserNo(@Param("userNo") Long userNo);

	// 특정 사용자 민감정보 삭제 (만료 시 레이지 정리 / 재인증 전 정리)
	int deleteSensitiveByUserNo(@Param("userNo") Long userNo);

	// 스케줄러: 보관 만료된 민감정보 일괄 삭제
	int deleteExpiredSensitiveInfos();
}
