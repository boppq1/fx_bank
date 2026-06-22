package com.example.bank.fx.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.bank.fx.dto.FxDataDto;


@Controller
public class FxRateController {

	@GetMapping("/api/viewRate")
	public List<FxDataDto> viewRate(){
		return ;
	}
}
