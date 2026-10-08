package com.ekyc.management.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_requests")
public class CustomerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "status")
    private String status;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "old_value", length = 1000)
    private String oldValue;

    @Column(name = "new_value", length = 1000)
    private String newValue;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "proof_file")
    private String proofFile;

    @Column(name = "proof_photo")
    private String proofPhoto;

    @Column(name = "live_face_photo")
    private String liveFacePhoto;

    @Column(name = "verification_type")
    private String verificationType;

    @Column(name = "verification_status")
    private String verificationStatus;

    @Column(name = "face_match_score")
    private Double faceMatchScore;

    @Column(name = "admin_remarks", length = 1000)
    private String adminRemarks;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    public CustomerRequest() {
    }

    @PrePersist
    public void beforeSave() {
        if (requestDate == null) {
            requestDate = LocalDateTime.now();
        }
    }

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

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
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

    public String getProofFile() {
        return proofFile;
    }

    public void setProofFile(String proofFile) {
        this.proofFile = proofFile;
    }

    public String getProofPhoto() {
        return proofPhoto;
    }

    public void setProofPhoto(String proofPhoto) {
        this.proofPhoto = proofPhoto;
    }

    public String getLiveFacePhoto() {
        return liveFacePhoto;
    }

    public void setLiveFacePhoto(String liveFacePhoto) {
        this.liveFacePhoto = liveFacePhoto;
    }

    public String getVerificationType() {
        return verificationType;
    }

    public void setVerificationType(String verificationType) {
        this.verificationType = verificationType;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Double getFaceMatchScore() {
        return faceMatchScore;
    }

    public void setFaceMatchScore(Double faceMatchScore) {
        this.faceMatchScore = faceMatchScore;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }
}