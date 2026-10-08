package com.ekyc.management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ekyc.management.model.Customer;
import com.ekyc.management.model.CustomerRequest;
import com.ekyc.management.repository.CustomerRepository;
import com.ekyc.management.repository.CustomerRequestRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class KycUpdateSuccessController {

    private final CustomerRepository customerRepository;
    private final CustomerRequestRepository customerRequestRepository;

    public KycUpdateSuccessController(
            CustomerRepository customerRepository,
            CustomerRequestRepository customerRequestRepository) {

        this.customerRepository = customerRepository;
        this.customerRequestRepository = customerRequestRepository;
    }

    // ============================================================
    // KYC UPDATE SUCCESS PAGE
    // ============================================================

    @GetMapping("/kyc-update-success")
    public String showKycUpdateSuccess(
            HttpSession session,
            Model model) {

        // ========================================================
        // 1. GET LOGGED-IN CUSTOMER
        // ========================================================

        String customerId =
                (String) session.getAttribute(
                        "loggedInCustomerId");

        if (customerId == null
                || customerId.trim().isEmpty()) {

            return "redirect:/login";
        }

        // ========================================================
        // 2. GET COMPLETED REQUEST ID
        // ========================================================

        Object requestIdObject =
                session.getAttribute(
                        "completedKycRequestId");

        if (requestIdObject == null) {

            model.addAttribute(
                    "error",
                    "KYC update request information was not found.");

            return "kyc-update-success";
        }

        Long requestId;

        try {

            requestId =
                    Long.parseLong(
                            requestIdObject.toString());

        } catch (NumberFormatException e) {

            model.addAttribute(
                    "error",
                    "Invalid KYC request ID.");

            return "kyc-update-success";
        }

        // ========================================================
        // 3. GET CUSTOMER
        // ========================================================

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElse(null);

        if (customer == null) {

            return "redirect:/login";
        }

        // ========================================================
        // 4. GET REQUEST
        // ========================================================

        CustomerRequest request =
                customerRequestRepository
                        .findById(requestId)
                        .orElse(null);

        if (request == null) {

            model.addAttribute(
                    "customer",
                    customer);

            model.addAttribute(
                    "error",
                    "Completed KYC request was not found.");

            return "kyc-update-success";
        }

        // ========================================================
        // 5. SECURITY CHECK
        // ========================================================

        if (request.getCustomerId() == null
                || !customerId.equals(
                        request.getCustomerId())) {

            return "redirect:/customer-dashboard";
        }

        // ========================================================
        // 6. ADD DATA TO PAGE
        // ========================================================

        model.addAttribute(
                "customer",
                customer);

        model.addAttribute(
                "request",
                request);

        model.addAttribute(
                "matchScore",
                request.getFaceMatchScore());

        model.addAttribute(
                "successMessage",
                "Face verification successful. Your KYC information has been updated successfully.");

        return "kyc-update-success";
    }
}