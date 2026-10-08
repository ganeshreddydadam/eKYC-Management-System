package com.ekyc.management.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "kyc_update_requests")
public class KycUpdateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "update_field", nullable = false)
    private String updateField;

    @Column(name = "current_value")
    private String currentValue;

    @Column(name = "new_value", nullable = false)
    private String newValue;

    @Column(name = "reason", nullable = false)
    private String reason;

    /*
     * Proof is NOT required for AADHAAR_MOBILE
     * because mobile number is verified using OTP.
     */
    @Column(name = "proof_type")
    private String proofType;

    @Column(name = "proof_file_name")
    private String proofFileName;

    @Column(name = "proof_file_path")
    private String proofFilePath;

    @Column(name = "status")
    private String status;

    @Column(name = "admin_remarks")
    private String adminRemarks;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    @Column(name = "verified_date")
    private LocalDateTime verifiedDate;


    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }


    public String getUpdateField() {
        return updateField;
    }

    public void setUpdateField(String updateField) {
        this.updateField = updateField;
    }


    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }


    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }


    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }


    public String getProofType() {
        return proofType;
    }

    public void setProofType(String proofType) {
        this.proofType = proofType;
    }


    public String getProofFileName() {
        return proofFileName;
    }

    public void setProofFileName(String proofFileName) {
        this.proofFileName = proofFileName;
    }


    public String getProofFilePath() {
        return proofFilePath;
    }

    public void setProofFilePath(String proofFilePath) {
        this.proofFilePath = proofFilePath;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }


    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }


    public LocalDateTime getVerifiedDate() {
        return verifiedDate;
    }

    public void setVerifiedDate(LocalDateTime verifiedDate) {
        this.verifiedDate = verifiedDate;
    }
}