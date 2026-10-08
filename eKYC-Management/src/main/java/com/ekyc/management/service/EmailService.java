package com.ekyc.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // =====================================================
    // SEND LOGIN OTP
    // =====================================================

    public void sendLoginOtp(
            String toEmail,
            String otp) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(toEmail);

        message.setSubject(
                "eKYC Management System - Login OTP"
        );

        message.setText(
                "Dear Customer,\n\n"
                + "Your OTP for eKYC login is: "
                + otp
                + "\n\n"
                + "This OTP is valid for 5 minutes.\n\n"
                + "Please do not share this OTP with anyone.\n\n"
                + "Regards,\n"
                + "eKYC Management System"
        );

        mailSender.send(message);
    }
}