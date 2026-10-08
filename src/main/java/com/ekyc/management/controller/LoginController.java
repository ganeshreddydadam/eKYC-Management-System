package com.ekyc.management.controller;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ekyc.management.model.Customer;
import com.ekyc.management.repository.CustomerRepository;
import com.ekyc.management.service.EmailService;

@Controller
public class LoginController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmailService emailService;


    // =====================================================
    // LOGIN PAGE
    // GET /login
    // =====================================================

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }


    // =====================================================
    // SEND OTP
    // POST /login
    // =====================================================

    @PostMapping("/login")
    public String sendOtp(
            @RequestParam String customerId,
            HttpSession session,
            Model model) {

        try {

            customerId = customerId.trim();

            // =================================================
            // CHECK CUSTOMER ID
            // =================================================

            if (customerId.isEmpty()) {

                model.addAttribute(
                        "error",
                        "Please enter your Customer ID."
                );

                return "login";
            }


            // =================================================
            // FIND CUSTOMER USING CUSTOMER ID
            // =================================================

            Customer customer =
                    customerRepository
                            .findByCustomerId(customerId)
                            .orElse(null);


            // =================================================
            // CUSTOMER NOT FOUND
            // =================================================

            if (customer == null) {

                model.addAttribute(
                        "error",
                        "Customer ID is not registered."
                );

                return "login";
            }


            // =================================================
            // GET REGISTERED EMAIL
            // =================================================

            String email = customer.getEmail();

            if (email == null ||
                    email.trim().isEmpty()) {

                model.addAttribute(
                        "error",
                        "No registered email address found for this Customer ID."
                );

                return "login";
            }

            email = email.trim().toLowerCase();


            // =================================================
            // GENERATE SECURE 6 DIGIT OTP
            // =================================================

            SecureRandom secureRandom =
                    new SecureRandom();

            String otp = String.format(
                    "%06d",
                    secureRandom.nextInt(1000000)
            );


            // =================================================
            // SAVE OTP IN SESSION
            // =================================================

            session.setAttribute(
                    "loginOtp",
                    otp
            );

            session.setAttribute(
                    "loginEmail",
                    email
            );

            session.setAttribute(
                    "customerId",
                    customer.getCustomerId()
            );

            session.setAttribute(
                    "otpCreatedTime",
                    LocalDateTime.now()
            );


            // =================================================
            // SEND OTP THROUGH SMTP
            // =================================================
            // OTP is NOT printed in Eclipse console.

            emailService.sendLoginOtp(
                    email,
                    otp
            );


            // =================================================
            // OPEN OTP PAGE
            // =================================================

            model.addAttribute(
                    "email",
                    email
            );

            return "otp";


        } catch (Exception e) {

            e.printStackTrace();

            model.addAttribute(
                    "error",
                    "Unable to send OTP. Please try again."
            );

            return "login";
        }
    }


    // =====================================================
    // OPEN OTP PAGE
    // GET /verify-otp
    // =====================================================

    @GetMapping("/verify-otp")
    public String showOtpPage(
            HttpSession session,
            Model model) {

        String email =
                (String) session.getAttribute(
                        "loginEmail"
                );

        if (email == null) {

            return "redirect:/login";
        }

        model.addAttribute(
                "email",
                email
        );

        return "otp";
    }


    // =====================================================
    // VERIFY OTP
    // POST /verify-otp
    // =====================================================

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam String otp,
            HttpSession session,
            Model model) {

        String sessionOtp =
                (String) session.getAttribute(
                        "loginOtp"
                );

        String email =
                (String) session.getAttribute(
                        "loginEmail"
                );

        String customerId =
                (String) session.getAttribute(
                        "customerId"
                );

        LocalDateTime otpCreatedTime =
                (LocalDateTime) session.getAttribute(
                        "otpCreatedTime"
                );


        // =================================================
        // SESSION EXPIRED
        // =================================================

        if (sessionOtp == null ||
                email == null ||
                customerId == null ||
                otpCreatedTime == null) {

            model.addAttribute(
                    "error",
                    "OTP session expired. Please login again."
            );

            return "login";
        }


        // =================================================
        // CHECK OTP EXPIRY
        // =================================================
        // OTP valid for 5 minutes.

        if (otpCreatedTime
                .plusMinutes(5)
                .isBefore(LocalDateTime.now())) {

            session.removeAttribute("loginOtp");
            session.removeAttribute("otpCreatedTime");

            model.addAttribute(
                    "error",
                    "OTP has expired. Please login again."
            );

            return "login";
        }


        // =================================================
        // CHECK OTP
        // =================================================

        if (otp == null ||
                !otp.trim().equals(sessionOtp)) {

            model.addAttribute(
                    "error",
                    "Invalid OTP. Please enter the correct OTP."
            );

            model.addAttribute(
                    "email",
                    email
            );

            return "otp";
        }


        // =================================================
        // OTP CORRECT
        // =================================================

        session.setAttribute(
                "loggedIn",
                true
        );

        session.setAttribute(
                "loggedInEmail",
                email
        );

        session.setAttribute(
                "loggedInCustomerId",
                customerId
        );


        // =================================================
        // REMOVE TEMPORARY OTP DATA
        // =================================================

        session.removeAttribute("loginOtp");
        session.removeAttribute("otpCreatedTime");
        session.removeAttribute("loginEmail");
        session.removeAttribute("customerId");


        // =================================================
        // GO TO CUSTOMER DASHBOARD
        // =================================================

        return "redirect:/customer-dashboard";
    }


    // =====================================================
    // LOGOUT
    // GET /logout
    // =====================================================

    @GetMapping("/logout")
    public String logout(
            HttpSession session) {

        session.invalidate();

        return "redirect:/";
    }
}