package com.example.bank.fx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class FxMainController {

	//@GetMapping("/")
	public String fxHome() {
		
		return "fx-home";
	}
	
}
