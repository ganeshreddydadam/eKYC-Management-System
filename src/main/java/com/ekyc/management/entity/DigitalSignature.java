package com.ekyc.management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "digital_signatures")
public class DigitalSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ekycId;

    private String customerName;

    private String certificateNumber;

    private String issuer;

    private LocalDateTime signedDate;

    private String status;

    private String verificationToken;


    // Default constructor
    public DigitalSignature() {
    }


    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Long getEkycId() {
        return ekycId;
    }

    public void setEkycId(Long ekycId) {
        this.ekycId = ekycId;
    }


    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }


    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(
            String certificateNumber) {

        this.certificateNumber =
                certificateNumber;
    }


    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }


    public LocalDateTime getSignedDate() {
        return signedDate;
    }

    public void setSignedDate(
            LocalDateTime signedDate) {

        this.signedDate = signedDate;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(
            String verificationToken) {

        this.verificationToken =
                verificationToken;
    }
}