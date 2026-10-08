package com.ekyc.management.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ekyc.management.model.Customer;
import com.ekyc.management.model.Customer.Gender;
import com.ekyc.management.repository.CustomerRepository;

@Controller
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    // =========================================================
    // REGISTER PAGE
    // =========================================================

    @GetMapping("/register")
    public String showRegistrationForm() {

        return "register";
    }

    // =========================================================
    // CUSTOMER REGISTRATION
    // =========================================================

    @PostMapping("/register")
    public String registerCustomer(

            @RequestParam String fullName,

            @RequestParam(required = false) String dateOfBirth,

            @RequestParam(required = false) String gender,

            @RequestParam String mobile,

            @RequestParam String email,

            @RequestParam(required = false) String address,

            @RequestParam(required = false) String city,

            @RequestParam(required = false) String state,

            @RequestParam(required = false) String pincode,

            @RequestParam(required = false) String aadhaarNumber,

            @RequestParam(required = false) String panNumber,

            Model model) {

        try {

            Customer customer = new Customer();

            // -------------------------------------------------
            // CLEAN INPUT
            // -------------------------------------------------

            fullName = fullName == null ? null : fullName.trim();

            mobile = mobile == null ? null : mobile.trim();

            email = email == null ? null : email.trim().toLowerCase();

            address = address == null ? null : address.trim();

            city = city == null ? null : city.trim();

            state = state == null ? null : state.trim();

            pincode = pincode == null ? null : pincode.trim();

            aadhaarNumber = aadhaarNumber == null
                    ? null
                    : aadhaarNumber.trim();

            panNumber = panNumber == null
                    ? null
                    : panNumber.trim().toUpperCase();

            // -------------------------------------------------
            // VALIDATE NAME
            // -------------------------------------------------

            if (fullName == null || fullName.isEmpty()) {

                model.addAttribute(
                        "error",
                        "Please enter your full name."
                );

                return "register";
            }

            // -------------------------------------------------
            // VALIDATE MOBILE
            // -------------------------------------------------

            if (mobile == null || mobile.isEmpty()) {

                model.addAttribute(
                        "error",
                        "Please enter your mobile number."
                );

                return "register";
            }

            // -------------------------------------------------
            // VALIDATE EMAIL
            // -------------------------------------------------

            if (email == null || email.isEmpty()) {

                model.addAttribute(
                        "error",
                        "Please enter your email address."
                );

                return "register";
            }

            // -------------------------------------------------
            // CHECK DUPLICATE EMAIL
            // -------------------------------------------------

            if (customerRepository.findByEmail(email).isPresent()) {

                model.addAttribute(
                        "error",
                        "This Email Address is already registered. Please use a different Email."
                );

                return "register";
            }

            // -------------------------------------------------
            // CHECK DUPLICATE MOBILE
            // -------------------------------------------------

            if (mobile != null
                    && !mobile.isEmpty()
                    && customerRepository.findByMobile(mobile).isPresent()) {

                model.addAttribute(
                        "error",
                        "This Mobile Number is already registered. Please use a different Mobile Number."
                );

                return "register";
            }

            // -------------------------------------------------
            // CHECK DUPLICATE AADHAAR
            // -------------------------------------------------

            if (aadhaarNumber != null
                    && !aadhaarNumber.isEmpty()
                    && customerRepository
                            .findByAadhaarNumber(aadhaarNumber)
                            .isPresent()) {

                model.addAttribute(
                        "error",
                        "This Aadhaar Number is already registered. Please use a different Aadhaar Number."
                );

                return "register";
            }

            // -------------------------------------------------
            // CHECK DUPLICATE PAN
            // -------------------------------------------------

            if (panNumber != null
                    && !panNumber.isEmpty()
                    && customerRepository
                            .findByPanNumber(panNumber)
                            .isPresent()) {

                model.addAttribute(
                        "error",
                        "This PAN Number is already registered. Please use a different PAN Number."
                );

                return "register";
            }

            // -------------------------------------------------
            // SET CUSTOMER DATA
            // -------------------------------------------------

            customer.setFullName(fullName);

            customer.setMobile(mobile);

            customer.setEmail(email);

            // -------------------------------------------------
            // DATE OF BIRTH
            // -------------------------------------------------

            if (dateOfBirth != null
                    && !dateOfBirth.trim().isEmpty()) {

                try {

                    customer.setDateOfBirth(
                            LocalDate.parse(dateOfBirth.trim())
                    );

                } catch (Exception e) {

                    model.addAttribute(
                            "error",
                            "Please enter a valid Date of Birth."
                    );

                    return "register";
                }
            }

            // -------------------------------------------------
            // GENDER
            // -------------------------------------------------

            if (gender != null
                    && !gender.trim().isEmpty()) {

                try {

                    customer.setGender(
                            Gender.valueOf(
                                    gender.trim().toUpperCase()
                            )
                    );

                } catch (IllegalArgumentException e) {

                    model.addAttribute(
                            "error",
                            "Please select a valid gender."
                    );

                    return "register";
                }
            }

            customer.setAddress(address);

            customer.setCity(city);

            customer.setState(state);

            customer.setPincode(pincode);

            customer.setAadhaarNumber(aadhaarNumber);

            customer.setPanNumber(panNumber);

            customer.setCreatedAt(LocalDateTime.now());

            // -------------------------------------------------
            // GENERATE CUSTOMER ID
            // -------------------------------------------------

            String customerId =
                    "CUS" + System.currentTimeMillis();

            customer.setCustomerId(customerId);

            // -------------------------------------------------
            // SAVE CUSTOMER
            // -------------------------------------------------

            customerRepository.save(customer);

            // -------------------------------------------------
            // REGISTRATION SUCCESS
            // -------------------------------------------------

            model.addAttribute(
                    "message",
                    "Customer Registration Successful!"
            );

            model.addAttribute(
                    "customerId",
                    customerId
            );

            model.addAttribute(
                    "customer",
                    customer
            );

            return "registration-success";

        } catch (DataIntegrityViolationException e) {

            e.printStackTrace();

            String errorMessage = "";

            if (e.getMostSpecificCause() != null) {

                errorMessage =
                        e.getMostSpecificCause().getMessage();
            }

            if (errorMessage != null
                    && errorMessage
                            .toLowerCase()
                            .contains("aadhaar_number")) {

                model.addAttribute(
                        "error",
                        "This Aadhaar Number is already registered. Please use a different Aadhaar Number."
                );

            } else if (errorMessage != null
                    && errorMessage
                            .toLowerCase()
                            .contains("pan_number")) {

                model.addAttribute(
                        "error",
                        "This PAN Number is already registered. Please use a different PAN Number."
                );

            } else if (errorMessage != null
                    && errorMessage
                            .toLowerCase()
                            .contains("email")) {

                model.addAttribute(
                        "error",
                        "This Email Address is already registered. Please use a different Email."
                );

            } else if (errorMessage != null
                    && errorMessage
                            .toLowerCase()
                            .contains("mobile")) {

                model.addAttribute(
                        "error",
                        "This Mobile Number is already registered. Please use a different Mobile Number."
                );

            } else {

                model.addAttribute(
                        "error",
                        "Registration failed. Some customer information already exists."
                );
            }

            return "register";

        } catch (Exception e) {

            e.printStackTrace();

            model.addAttribute(
                    "error",
                    "Registration failed. Please check your details and try again."
            );

            return "register";
        }
    }

    // =========================================================
    // UPDATE KYC INFORMATION PAGE
    // =========================================================

    @GetMapping("/update-kyc-information")
    public String updateKycInformation(

            @RequestParam(required = false) String customerId,

            Model model) {

        if (customerId == null
                || customerId.trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Customer ID is missing. Please open KYC Update from your Customer Dashboard."
            );

            return "kyc-update-request";
        }

        customerId = customerId.trim();

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElse(null);

        if (customer == null) {

            model.addAttribute(
                    "error",
                    "Customer not found."
            );

            return "kyc-update-request";
        }

        model.addAttribute(
                "customerId",
                customerId
        );

        model.addAttribute(
                "customer",
                customer
        );

        return "kyc-update-request";
    }

    // =========================================================
    // SHOW KYC UPDATE REQUEST PAGE
    // =========================================================

    @GetMapping("/kyc-update-request")
    public String showKycUpdateRequest(

            @RequestParam(required = false) String customerId,

            Model model) {

        if (customerId == null
                || customerId.trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Customer ID is missing."
            );

            return "kyc-update-request";
        }

        customerId = customerId.trim();

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElse(null);

        if (customer == null) {

            model.addAttribute(
                    "error",
                    "Customer not found."
            );

            return "kyc-update-request";
        }

        model.addAttribute(
                "customerId",
                customerId
        );

        model.addAttribute(
                "customer",
                customer
        );

        return "kyc-update-request";
    }

    // =========================================================
    // IMPORTANT:
    // DO NOT ADD @PostMapping("/kyc-update-request") HERE
    //
    // The POST /kyc-update-request mapping is already handled
    // by CustomerRequestController.
    // =========================================================
}