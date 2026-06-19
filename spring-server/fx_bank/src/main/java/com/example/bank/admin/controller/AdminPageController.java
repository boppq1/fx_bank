package com.example.bank.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @GetMapping("/pro/list")
    public String proList() {
        return "admin/adminProList";
    }

    @GetMapping("/pro/detail")
    public String proDetail() {
        return "admin/adminProDetail";
    }

    @GetMapping("/terms/register")
    public String register() {
        return "admin/adminTermsRegister";
    }

    @GetMapping("/terms/version")
    public String version() {
        return "admin/adminVersionHis";
    }
}