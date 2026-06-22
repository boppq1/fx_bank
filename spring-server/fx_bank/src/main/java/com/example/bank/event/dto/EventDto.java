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
public class EventDto {
	private Long eventNo;
	private Long userNo;
	private String b; // Y, N
	private String n; // Y, N
	private String k; // Y, N
	private String applied; // Y, N
	private LocalDateTime createdDt;
}
