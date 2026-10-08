package com.ekyc.management.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ekyc.management.model.CustomerRequest;
import com.ekyc.management.repository.CustomerRequestRepository;

@Controller
public class AdminDashboardController {

    @Autowired
    private CustomerRequestRepository customerRequestRepository;

    // ============================================================
    // ADMIN DASHBOARD
    // ============================================================

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {

        // Get ALL customer KYC requests.
        // We do not use PENDING because admin does not
        // approve or reject requests anymore.

        List<CustomerRequest> requests =
                customerRequestRepository.findAll();

        model.addAttribute(
                "requests",
                requests
        );

        return "admin-dashboard";
    }
}