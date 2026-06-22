package com.example.bank.event.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CouponDto {
	private Long couponNo;
	private Long userNo;
	private Long eventNo;
	private Long productNo;
	private Long preferentialRate;
	private String isUsed;
	private LocalDateTime issuedDt;
	private LocalDateTime usedDt;
}
