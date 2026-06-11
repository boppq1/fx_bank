package com.example.bank.fx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FxMainController {

	@GetMapping("/")
	public String fxHome() {
		
		return "fxHome";
	}
	
}
