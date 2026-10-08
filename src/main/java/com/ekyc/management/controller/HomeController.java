package com.ekyc.management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // =====================================================
    // HOME PAGE
    // GET /
    // =====================================================

    @GetMapping("/")
    public String home() {
        return "home";
    }
}