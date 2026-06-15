/**
 * ========================================================================
 * 파일명: ViewController.java
 * 작성 위치: com.fxbank.domain.user.controller
 * 역할: 타임리프 동적 템플릿(HTML) 화면을 매핑하여 열어주는 컨트롤러
 * 설명:
 * - 데이터(JSON)가 아닌 화면(HTML)을 반환하므로 @RestController가 아닌 일반 @Controller를 사용합니다.
 * ========================================================================
 */
package com.example.bank.personal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

	@GetMapping("/register")
    public String registerPage() {
        return "register"; // src/main/resources/templates/register.html을 뷰리졸버가 찾아감
    }
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // templates/login.html 파일명과 매칭됨
    }
 
    //@GetMapping("/")
    public String mainPage() {
        // templates 폴더 안에 있는 "index.html" 파일을 화면에 그려라!
        return "index"; 
    }
    
    @GetMapping("/a")
    public String aPage() {
    	// templates 폴더 안에 있는 "index.html" 파일을 화면에 그려라!
    	return "a"; 
    }
    
}