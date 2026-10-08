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
                            "loggedInCustomerId");

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
                        "Customer information not found.");

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
                    customerId);

            request.setRequestType(
                    requestType);

            request.setMessage(
                    message);

            // =====================================================
            // FIELD NAME
            // =====================================================

            if (fieldName != null &&
                    !fieldName.trim().isEmpty()) {

                request.setFieldName(
                        fieldName
                                .trim()
                                .toUpperCase());
            }

            // =====================================================
            // OLD VALUE
            // =====================================================

            if (oldValue != null &&
                    !oldValue.trim().isEmpty()) {

                request.setOldValue(
                        oldValue.trim());
            }

            // =====================================================
            // NEW VALUE
            // =====================================================

            if (newValue != null &&
                    !newValue.trim().isEmpty()) {

                request.setNewValue(
                        newValue.trim());
            }

            // =====================================================
            // REASON
            // =====================================================

            if (reason != null &&
                    !reason.isEmpty()) {

                request.setReason(
                        reason);
            }

            // =====================================================
            // VERIFICATION TYPE
            // =====================================================

            if (verificationType != null &&
                    !verificationType.isEmpty()) {

                request.setVerificationType(
                        verificationType);
            }

            // =====================================================
            // PROOF FILE
            // =====================================================

            /*
             * This controller only creates the initial request.
             *
             * The actual proof upload is handled by:
             *
             * KycUpdateController
             *
             * through:
             *
             * /submit-kyc-update
             */

            if (proofFile != null &&
                    !proofFile.isEmpty()) {

                String originalFilename =
                        proofFile.getOriginalFilename();

                if (originalFilename != null &&
                        !originalFilename.trim().isEmpty()) {

                    request.setProofFile(
                            originalFilename);
                }
            }

            // =====================================================
            // 6. STATUS
            // =====================================================

            request.setStatus(
                    "PENDING");

            request.setVerificationStatus(
                    "PENDING");

            request.setRequestDate(
                    LocalDateTime.now());

            // =====================================================
            // 7. SAVE REQUEST
            // =====================================================

            customerRequestRepository.save(
                    request);

            // =====================================================
            // 8. GET GENERATED REQUEST ID
            // =====================================================

            Long requestId =
                    request.getId();

            // =====================================================
            // 9. SAVE REQUEST ID IN SESSION
            // =====================================================

            session.setAttribute(
                    "kycRequestId",
                    requestId);

            // =====================================================
            // 10. SUCCESS MESSAGE
            // =====================================================

            session.setAttribute(
                    "requestSuccess",
                    "Request created successfully.");

            session.removeAttribute(
                    "requestError");

            // =====================================================
            // IMPORTANT CHANGE
            // =====================================================
            //
            // OLD:
            //
            // return "redirect:/kyc-update";
            //
            // NEW:
            //
            // Go directly to the new KYC update form.
            //
            // =====================================================

            return "redirect:/kyc-update-form";

        } catch (Exception e) {

            e.printStackTrace();

            session.setAttribute(
                    "requestError",
                    "Unable to submit request. Please try again.");

            session.removeAttribute(
                    "requestSuccess");

            return "redirect:/customer-dashboard";
        }
    }

    // =========================================================
    // OPEN KYC UPDATE PAGE
    // =========================================================
    //
    // IMPORTANT:
    // The old kyc-update.html page must no longer be displayed.
    //
    // We redirect it to the new kyc-update-form.html.
    //
    // =========================================================

    @GetMapping("/kyc-update")
    public String kycUpdatePage() {

        return "redirect:/kyc-update-form";
    }

    // =========================================================
    // OPEN KYC UPDATE FORM
    // =========================================================

    @GetMapping("/kyc-update-form")
    public String showKycUpdateForm(
            HttpSession session,
            Model model) {

        // =====================================================
        // 1. GET LOGGED-IN CUSTOMER
        // =====================================================

        String customerId =
                (String) session.getAttribute(
                        "loggedInCustomerId");

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

            session.invalidate();

            return "redirect:/login";
        }

        // =====================================================
        // 3. GET REQUEST ID
        // =====================================================

        Object requestIdObject =
                session.getAttribute(
                        "kycRequestId");

        Long requestId = null;

        if (requestIdObject != null) {

            try {

                requestId =
                        Long.parseLong(
                                requestIdObject.toString());

            } catch (NumberFormatException e) {

                requestId = null;
            }
        }

        // =====================================================
        // 4. REQUEST MUST EXIST
        // =====================================================

        if (requestId == null) {

            session.setAttribute(
                    "kycUpdateError",
                    "Please create a KYC update request first.");

            return "redirect:/customer-dashboard";
        }

        // =====================================================
        // 5. ADD CUSTOMER TO PAGE
        // =====================================================

        model.addAttribute(
                "customer",
                customer);

        // =====================================================
        // 6. SUCCESS MESSAGE
        // =====================================================

        String successMessage =
                (String) session.getAttribute(
                        "requestSuccess");

        if (successMessage != null) {

            model.addAttribute(
                    "successMessage",
                    successMessage);

            session.removeAttribute(
                    "requestSuccess");
        }

        String kycSuccessMessage =
                (String) session.getAttribute(
                        "kycUpdateSuccess");

        if (kycSuccessMessage != null) {

            model.addAttribute(
                    "successMessage",
                    kycSuccessMessage);

            session.removeAttribute(
                    "kycUpdateSuccess");
        }

        // =====================================================
        // 7. ERROR MESSAGE
        // =====================================================

        String errorMessage =
                (String) session.getAttribute(
                        "requestError");

        if (errorMessage != null) {

            model.addAttribute(
                    "errorMessage",
                    errorMessage);

            session.removeAttribute(
                    "requestError");
        }

        String kycErrorMessage =
                (String) session.getAttribute(
                        "kycUpdateError");

        if (kycErrorMessage != null) {

            model.addAttribute(
                    "errorMessage",
                    kycErrorMessage);

            session.removeAttribute(
                    "kycUpdateError");
        }

        // =====================================================
        // 8. RETURN NEW FORM
        // =====================================================

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
                        "loggedInCustomerId");

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
                                customerId);

        model.addAttribute(
                "customer",
                customer);

        model.addAttribute(
                "requests",
                requests);

        return "customer-requests";
    }
}