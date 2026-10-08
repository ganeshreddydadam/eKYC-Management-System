package com.ekyc.management.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MobileOtpController {

    private static final int OTP_EXPIRY_MINUTES = 5;

    private final Random random = new Random();

    // =========================================================
    // SEND MOBILE OTP
    // =========================================================

    @PostMapping("/send-mobile-otp")
    public String sendMobileOtp(
            @RequestParam("mobile") String mobile,
            HttpSession session) {

        try {

            // -------------------------------------------------
            // Validate mobile number
            // -------------------------------------------------

            if (mobile == null) {
                return "INVALID_MOBILE";
            }

            mobile = mobile.trim();

            if (!mobile.matches("^[6-9][0-9]{9}$")) {
                return "INVALID_MOBILE";
            }

            // -------------------------------------------------
            // Generate 6-digit OTP
            // -------------------------------------------------

            String otp = String.format(
                    "%06d",
                    random.nextInt(1_000_000)
            );

            // -------------------------------------------------
            // Store OTP in session
            // -------------------------------------------------

            session.setAttribute(
                    "mobileOtp",
                    otp
            );

            // -------------------------------------------------
            // Store mobile number associated with OTP
            // -------------------------------------------------

            session.setAttribute(
                    "otpMobile",
                    mobile
            );

            // -------------------------------------------------
            // Store OTP creation time
            // -------------------------------------------------

            session.setAttribute(
                    "mobileOtpCreatedTime",
                    LocalDateTime.now()
            );

            // -------------------------------------------------
            // Reset verification status
            // -------------------------------------------------

            session.setAttribute(
                    "mobileOtpVerified",
                    false
            );

            // Remove any previously verified mobile
            session.removeAttribute(
                    "verifiedMobile"
            );

            // -------------------------------------------------
            // TESTING ONLY
            // OTP appears in Eclipse console
            // -------------------------------------------------

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "           MOBILE OTP"
            );

            System.out.println(
                    "Mobile : " + mobile
            );

            System.out.println(
                    "OTP    : " + otp
            );

            System.out.println(
                    "Expires: " +
                    LocalDateTime.now()
                            .plusMinutes(OTP_EXPIRY_MINUTES)
            );

            System.out.println(
                    "======================================"
            );

            return "OTP_SENT";

        } catch (Exception e) {

            e.printStackTrace();

            return "OTP_SEND_ERROR";
        }
    }


    // =========================================================
    // VERIFY MOBILE OTP
    // =========================================================

    @PostMapping("/verify-mobile-otp")
    public String verifyMobileOtp(
            @RequestParam("mobile") String mobile,
            @RequestParam("otp") String otp,
            HttpSession session) {

        try {

            // -------------------------------------------------
            // Validate mobile
            // -------------------------------------------------

            if (mobile == null) {
                return "INVALID_MOBILE";
            }

            mobile = mobile.trim();

            if (!mobile.matches("^[6-9][0-9]{9}$")) {
                return "INVALID_MOBILE";
            }

            // -------------------------------------------------
            // Validate OTP input
            // -------------------------------------------------

            if (otp == null) {
                return "INVALID_OTP";
            }

            otp = otp.trim();

            if (!otp.matches("^[0-9]{6}$")) {
                return "INVALID_OTP";
            }

            // -------------------------------------------------
            // Get OTP from session
            // -------------------------------------------------

            String savedOtp =
                    (String) session.getAttribute(
                            "mobileOtp"
                    );

            // -------------------------------------------------
            // Get mobile associated with OTP
            // -------------------------------------------------

            String savedMobile =
                    (String) session.getAttribute(
                            "otpMobile"
                    );

            // -------------------------------------------------
            // Get OTP creation time
            // -------------------------------------------------

            LocalDateTime createdTime =
                    (LocalDateTime) session.getAttribute(
                            "mobileOtpCreatedTime"
                    );

            // -------------------------------------------------
            // OTP does not exist
            // -------------------------------------------------

            if (savedOtp == null ||
                    savedMobile == null ||
                    createdTime == null) {

                session.setAttribute(
                        "mobileOtpVerified",
                        false
                );

                return "OTP_EXPIRED";
            }

            // -------------------------------------------------
            // Check OTP expiry
            // -------------------------------------------------

            long minutesPassed =
                    Duration.between(
                            createdTime,
                            LocalDateTime.now()
                    ).toMinutes();

            if (minutesPassed >= OTP_EXPIRY_MINUTES) {

                clearOtpSession(session);

                return "OTP_EXPIRED";
            }

            // -------------------------------------------------
            // Check mobile number
            // -------------------------------------------------

            if (!savedMobile.equals(mobile)) {

                session.setAttribute(
                        "mobileOtpVerified",
                        false
                );

                return "MOBILE_MISMATCH";
            }

            // -------------------------------------------------
            // Check OTP
            // -------------------------------------------------

            if (!savedOtp.equals(otp)) {

                session.setAttribute(
                        "mobileOtpVerified",
                        false
                );

                return "INVALID_OTP";
            }

            // =================================================
            // OTP VERIFIED SUCCESSFULLY
            // =================================================

            session.setAttribute(
                    "mobileOtpVerified",
                    true
            );

            session.setAttribute(
                    "verifiedMobile",
                    mobile
            );

            // -------------------------------------------------
            // Remove OTP itself after successful verification
            // -------------------------------------------------

            session.removeAttribute(
                    "mobileOtp"
            );

            session.removeAttribute(
                    "otpMobile"
            );

            session.removeAttribute(
                    "mobileOtpCreatedTime"
            );

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "      MOBILE OTP VERIFIED"
            );

            System.out.println(
                    "Mobile : " + mobile
            );

            System.out.println(
                    "======================================"
            );

            return "OTP_VERIFIED";

        } catch (Exception e) {

            e.printStackTrace();

            session.setAttribute(
                    "mobileOtpVerified",
                    false
            );

            return "OTP_VERIFY_ERROR";
        }
    }


    // =========================================================
    // CLEAR OTP SESSION
    // =========================================================

    private void clearOtpSession(HttpSession session) {

        session.removeAttribute(
                "mobileOtp"
        );

        session.removeAttribute(
                "otpMobile"
        );

        session.removeAttribute(
                "mobileOtpCreatedTime"
        );

        session.removeAttribute(
                "mobileOtpVerified"
        );

        session.removeAttribute(
                "verifiedMobile"
        );
    }
}