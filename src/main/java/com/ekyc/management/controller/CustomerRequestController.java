package com.ekyc.management.controller;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ekyc.management.model.Customer;
import com.ekyc.management.model.CustomerRequest;
import com.ekyc.management.repository.CustomerRepository;
import com.ekyc.management.repository.CustomerRequestRepository;

@Controller
public class CustomerRequestController {

    @Autowired
    private CustomerRequestRepository customerRequestRepository;

    @Autowired
    private CustomerRepository customerRepository;


    // =========================================================
    // SUBMIT GENERAL CUSTOMER REQUEST
    // =========================================================

    @PostMapping("/customer-request")
    public String submitRequest(

            @RequestParam(value = "requestType", required = false)
            String requestType,

            @RequestParam(value = "message", required = false)
            String message,

            @RequestParam(value = "fieldName", required = false)
            String fieldName,

            @RequestParam(value = "oldValue", required = false)
            String oldValue,

            @RequestParam(value = "newValue", required = false)
            String newValue,

            @RequestParam(value = "reason", required = false)
            String reason,

            @RequestParam(value = "verificationType", required = false)
            String verificationType,

            @RequestParam(value = "proofFile", required = false)
            MultipartFile proofFile,

            HttpSession session) {

        try {

            // =====================================================
            // 1. GET LOGGED-IN CUSTOMER
            // =====================================================

            String customerId =
                    (String) session.getAttribute(
                            "loggedInCustomerId"
                    );

            if (customerId == null ||
                    customerId.trim().isEmpty()) {

                return "redirect:/login";
            }

            customerId = customerId.trim();


            // =====================================================
            // 2. FIND CUSTOMER
            // =====================================================

            Customer customer =
                    customerRepository
                            .findByCustomerId(customerId)
                            .orElse(null);

            if (customer == null) {

                session.setAttribute(
                        "requestError",
                        "Customer information not found."
                );

                return "redirect:/customer-dashboard";
            }


            // =====================================================
            // 3. GENERAL REQUEST VALUES
            // =====================================================

            if (requestType == null ||
                    requestType.trim().isEmpty()) {

                requestType = "GENERAL REQUEST";
            }

            requestType = requestType.trim();


            if (message == null ||
                    message.trim().isEmpty()) {

                message = "Customer request";
            }

            message = message.trim();


            if (reason != null) {
                reason = reason.trim();
            }


            if (verificationType != null) {

                verificationType =
                        verificationType
                                .trim()
                                .toUpperCase();
            }


            // =====================================================
            // 4. CREATE REQUEST
            // =====================================================

            CustomerRequest request =
                    new CustomerRequest();


            request.setCustomerId(
                    customerId
            );


            request.setRequestType(
                    requestType
            );


            request.setMessage(
                    message
            );


            if (fieldName != null &&
                    !fieldName.trim().isEmpty()) {

                request.setFieldName(
                        fieldName.trim().toUpperCase()
                );
            }


            if (oldValue != null &&
                    !oldValue.trim().isEmpty()) {

                request.setOldValue(
                        oldValue.trim()
                );
            }


            if (newValue != null &&
                    !newValue.trim().isEmpty()) {

                request.setNewValue(
                        newValue.trim()
                );
            }


            if (reason != null &&
                    !reason.isEmpty()) {

                request.setReason(
                        reason
                );
            }


            if (verificationType != null &&
                    !verificationType.isEmpty()) {

                request.setVerificationType(
                        verificationType
                );
            }


            // =====================================================
            // 5. PROOF FILE
            // =====================================================

            if (proofFile != null &&
                    !proofFile.isEmpty()) {

                String originalFilename =
                        proofFile.getOriginalFilename();


                if (originalFilename != null &&
                        !originalFilename.trim().isEmpty()) {

                    request.setProofFile(
                            originalFilename
                    );
                }
            }


            // =====================================================
            // 6. STATUS
            // =====================================================

            request.setStatus(
                    "PENDING"
            );


            request.setVerificationStatus(
                    "PENDING"
            );


            request.setRequestDate(
                    LocalDateTime.now()
            );


            // =====================================================
            // 7. SAVE FIRST REQUEST
            // =====================================================

            customerRequestRepository.save(
                    request
            );


            // =====================================================
            // IMPORTANT:
            // SAVE THE GENERATED ID OF THIS SAME REQUEST
            // =====================================================

            Long requestId = request.getId();

            session.setAttribute(
                    "kycRequestId",
                    requestId
            );


            // =====================================================
            // 8. SUCCESS
            // =====================================================

            session.setAttribute(
                    "requestSuccess",
                    "Request submitted successfully."
            );


            session.removeAttribute(
                    "requestError"
            );


            return "redirect:/kyc-update";


        } catch (Exception e) {

            e.printStackTrace();


            session.setAttribute(
                    "requestError",
                    "Unable to submit request. Please try again."
            );


            session.removeAttribute(
                    "requestSuccess"
            );


            return "redirect:/customer-dashboard";
        }
    }


    // =========================================================
    // OPEN KYC UPDATE PAGE
    // =========================================================

    @GetMapping("/kyc-update")
    public String kycUpdatePage(

            HttpSession session,
            Model model) {

        String customerId =
                (String) session.getAttribute(
                        "loggedInCustomerId"
                );


        if (customerId == null ||
                customerId.trim().isEmpty()) {

            return "redirect:/login";
        }


        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElse(null);


        if (customer == null) {

            session.invalidate();

            return "redirect:/login";
        }


        model.addAttribute(
                "customer",
                customer
        );


        String successMessage =
                (String) session.getAttribute(
                        "requestSuccess"
                );


        if (successMessage != null) {

            model.addAttribute(
                    "successMessage",
                    successMessage
            );


            session.removeAttribute(
                    "requestSuccess"
            );
        }


        String errorMessage =
                (String) session.getAttribute(
                        "requestError"
                );


        if (errorMessage != null) {

            model.addAttribute(
                    "errorMessage",
                    errorMessage
            );


            session.removeAttribute(
                    "requestError"
            );
        }


        return "kyc-update";
    }


    // =========================================================
    // OPEN KYC UPDATE FORM
    // =========================================================

    @GetMapping("/kyc-update-form")
    public String showKycUpdateForm(

            HttpSession session,
            Model model) {

        String customerId =
                (String) session.getAttribute(
                        "loggedInCustomerId"
                );


        if (customerId == null ||
                customerId.trim().isEmpty()) {

            return "redirect:/login";
        }


        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElse(null);


        if (customer == null) {

            session.invalidate();

            return "redirect:/login";
        }


        // =====================================================
        // MAKE SURE THERE IS AN EXISTING REQUEST
        // =====================================================

        Long requestId =
                (Long) session.getAttribute(
                        "kycRequestId"
                );


        if (requestId == null) {

            session.setAttribute(
                    "kycUpdateError",
                    "Please create a request first."
            );

            return "redirect:/customer-dashboard";
        }


        model.addAttribute(
                "customer",
                customer
        );


        String successMessage =
                (String) session.getAttribute(
                        "kycUpdateSuccess"
                );


        if (successMessage != null) {

            model.addAttribute(
                    "successMessage",
                    successMessage
            );


            session.removeAttribute(
                    "kycUpdateSuccess"
            );
        }


        String errorMessage =
                (String) session.getAttribute(
                        "kycUpdateError"
                );


        if (errorMessage != null) {

            model.addAttribute(
                    "errorMessage",
                    errorMessage
            );


            session.removeAttribute(
                    "kycUpdateError"
            );
        }


        return "kyc-update-form";
    }


    // =========================================================
    // VIEW CUSTOMER REQUESTS
    // =========================================================

    @GetMapping("/customer-requests")
    public String viewCustomerRequests(

            HttpSession session,
            Model model) {

        String customerId =
                (String) session.getAttribute(
                        "loggedInCustomerId"
                );


        if (customerId == null ||
                customerId.trim().isEmpty()) {

            return "redirect:/login";
        }


        customerId =
                customerId.trim();


        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElse(null);


        if (customer == null) {

            session.invalidate();

            return "redirect:/login";
        }


        List<CustomerRequest> requests =
                customerRequestRepository
                        .findByCustomerIdOrderByRequestDateDesc(
                                customerId
                        );


        model.addAttribute(
                "customer",
                customer
        );


        model.addAttribute(
                "requests",
                requests
        );


        return "customer-requests";
    }
}