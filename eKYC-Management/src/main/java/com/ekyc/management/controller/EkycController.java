package com.ekyc.management.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ekyc.management.model.Customer;
import com.ekyc.management.model.Ekyc;
import com.ekyc.management.repository.CustomerRepository;
import com.ekyc.management.repository.EkycRepository;

@Controller
public class EkycController {

    @Autowired
    private EkycRepository ekycRepository;

    @Autowired
    private CustomerRepository customerRepository;


    // =====================================================
    // PROCEED TO eKYC
    // =====================================================

    @GetMapping("/proceed-ekyc")
    public String proceedToEkyc(
            @RequestParam(required = false) String customerId) {

        if (customerId != null &&
                !customerId.trim().isEmpty()) {

            return "redirect:/ekyc?customerId="
                    + customerId.trim();
        }

        return "redirect:/ekyc";
    }


    // =====================================================
    // OPEN eKYC PAGE
    // =====================================================

    @GetMapping("/ekyc")
    public String showEkycPage(
            @RequestParam(required = false) String customerId,
            Model model) {

        model.addAttribute("customerId", customerId);

        return "ekyc";
    }


    // =====================================================
    // SUBMIT eKYC
    // =====================================================

    @PostMapping("/ekyc")
    public String submitEkyc(

            @RequestParam(required = false)
            String customerId,

            @RequestParam(required = false)
            String aadhaarNumber,

            @RequestParam(required = false)
            String panNumber,

            Model model) {

        try {

            // =================================================
            // 1. VALIDATE CUSTOMER ID
            // =================================================

            if (customerId == null ||
                    customerId.trim().isEmpty()) {

                model.addAttribute(
                        "error",
                        "Customer ID is missing. Please open eKYC from your customer dashboard."
                );

                return "ekyc";
            }

            customerId = customerId.trim();


            // =================================================
            // 2. FIND REGISTERED CUSTOMER
            // =================================================

            Customer customer =
                    customerRepository
                            .findByCustomerId(customerId)
                            .orElse(null);


            if (customer == null) {

                model.addAttribute(
                        "error",
                        "Customer ID not found."
                );

                model.addAttribute(
                        "customerId",
                        customerId
                );

                return "ekyc";
            }


            // =================================================
            // 3. GET REGISTERED AADHAAR AND PAN
            // =================================================

            String registeredAadhaar =
                    customer.getAadhaarNumber();

            String registeredPan =
                    customer.getPanNumber();


            // =================================================
            // 4. CHECK REGISTERED AADHAAR EXISTS
            // =================================================

            if (registeredAadhaar == null ||
                    registeredAadhaar.trim().isEmpty()) {

                model.addAttribute(
                        "error",
                        "No Aadhaar number is registered for this customer."
                );

                model.addAttribute(
                        "customerId",
                        customerId
                );

                return "ekyc";
            }


            // =================================================
            // 5. CHECK REGISTERED PAN EXISTS
            // =================================================

            if (registeredPan == null ||
                    registeredPan.trim().isEmpty()) {

                model.addAttribute(
                        "error",
                        "No PAN number is registered for this customer."
                );

                model.addAttribute(
                        "customerId",
                        customerId
                );

                return "ekyc";
            }


            // =================================================
            // 6. VALIDATE ENTERED AADHAAR
            // =================================================

            if (aadhaarNumber == null ||
                    aadhaarNumber.trim().isEmpty()) {

                model.addAttribute(
                        "error",
                        "Please enter your Aadhaar number."
                );

                model.addAttribute(
                        "customerId",
                        customerId
                );

                return "ekyc";
            }

            aadhaarNumber = aadhaarNumber.trim();


            // =================================================
            // 7. AADHAAR MUST MATCH REGISTERED AADHAAR
            // =================================================

            if (!aadhaarNumber.equals(
                    registeredAadhaar.trim())) {

                model.addAttribute(
                        "error",
                        "Aadhaar number does not match your registered Aadhaar number."
                );

                model.addAttribute(
                        "customerId",
                        customerId
                );

                return "ekyc";
            }


            // =================================================
            // 8. VALIDATE ENTERED PAN
            // =================================================

            if (panNumber == null ||
                    panNumber.trim().isEmpty()) {

                model.addAttribute(
                        "error",
                        "Please enter your PAN number."
                );

                model.addAttribute(
                        "customerId",
                        customerId
                );

                return "ekyc";
            }

            panNumber = panNumber.trim().toUpperCase();


            // =================================================
            // 9. PAN MUST MATCH REGISTERED PAN
            // =================================================

            if (!panNumber.equals(
                    registeredPan.trim().toUpperCase())) {

                model.addAttribute(
                        "error",
                        "PAN number does not match your registered PAN number."
                );

                model.addAttribute(
                        "customerId",
                        customerId
                );

                return "ekyc";
            }


            // =================================================
            // 10. BOTH MATCH
            // =================================================

            Ekyc ekyc = new Ekyc();

            ekyc.setCustomerId(customerId);

            ekyc.setAadhaarNumber(
                    aadhaarNumber
            );

            ekyc.setPanNumber(
                    panNumber
            );

            ekyc.setStatus("SUBMITTED");

            ekyc.setSubmittedAt(
                    LocalDateTime.now()
            );


            // =================================================
            // 11. SAVE eKYC
            // =================================================

            ekycRepository.save(ekyc);


            // =================================================
            // 12. SUCCESS
            // =================================================

            model.addAttribute(
                    "customerId",
                    customerId
            );

            model.addAttribute(
                    "message",
                    "eKYC submitted successfully!"
            );

            return "ekyc-success";


        } catch (Exception e) {

            e.printStackTrace();

            model.addAttribute(
                    "error",
                    "eKYC submission failed: "
                            + e.getMessage()
            );

            model.addAttribute(
                    "customerId",
                    customerId
            );

            return "ekyc";
        }
    }
}