package com.example.bank.fx.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FxDataDto {
	
	private int fx_rate_no;
	private String currency_code;
	private Double buy_rate;
	private Double sell_rate;
	private Double base_rate;
	private LocalDate announced_at;

}
