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

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {

        List<CustomerRequest> pendingRequests =
                customerRequestRepository.findByStatus("PENDING");

        model.addAttribute("pendingRequests", pendingRequests);

        return "admin-dashboard";
    }
}