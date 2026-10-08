package com.ekyc.management.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ekyc.management.model.Customer;
import com.ekyc.management.model.CustomerRequest;
import com.ekyc.management.repository.CustomerRepository;
import com.ekyc.management.repository.CustomerRequestRepository;

@Controller
public class KycUpdateController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerRequestRepository customerRequestRepository;

    private static final String PROOF_DIRECTORY =
            "uploads/proofs";


    // =========================================================
    // SUBMIT KYC UPDATE
    // =========================================================

    @PostMapping("/submit-kyc-update")
    public synchronized String submitKycUpdate(

            // =====================================================
            // SAME REQUEST ID CREATED IN FIRST STEP
            // =====================================================

            @RequestParam(value = "requestId", required = false)
            Long requestId,

            @RequestParam("updateField")
            String updateField,

            @RequestParam(value = "newValue", required = false)
            String newValue,

            @RequestParam(value = "reason", required = false)
            String reason,

            @RequestParam(value = "additionalMessage", required = false)
            String additionalMessage,

            @RequestParam(value = "proofType", required = false)
            String proofType,

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


            customerId =
                    customerId.trim();


            // =====================================================
            // 2. GET THE REQUEST ID
            // =====================================================
            // CustomerRequestController saves the generated request ID
            // in the session after creating the first request.
            // If the HTML form sends requestId, use that value.
            // Otherwise, recover the same ID from the session.

            if (requestId == null) {

                Object sessionRequestId =
                        session.getAttribute("kycRequestId");

                if (sessionRequestId instanceof Number) {

                    requestId =
                            ((Number) sessionRequestId).longValue();

                } else if (sessionRequestId != null) {

                    try {

                        requestId =
                                Long.parseLong(
                                        sessionRequestId.toString()
                                );

                    } catch (NumberFormatException e) {

                        requestId = null;
                    }
                }
            }

            if (requestId == null) {

                session.setAttribute(
                        "kycUpdateError",
                        "Please create a request first."
                );

                return "redirect:/customer-dashboard";
            }


            // =====================================================
            // 3. FIND THE EXISTING REQUEST
            // =====================================================

            CustomerRequest request =
                    customerRequestRepository
                            .findById(requestId)
                            .orElse(null);


            if (request == null) {

                session.setAttribute(
                        "kycUpdateError",
                        "KYC request not found."
                );

                return "redirect:/kyc-update-form";
            }


            // =====================================================
            // SECURITY:
            // REQUEST MUST BELONG TO LOGGED-IN CUSTOMER
            // =====================================================

            if (request.getCustomerId() == null ||
                    !request.getCustomerId()
                            .equals(customerId)) {

                session.setAttribute(
                        "kycUpdateError",
                        "Invalid KYC request."
                );

                return "redirect:/kyc-update-form";
            }


            // =====================================================
            // REQUEST MUST STILL BE PENDING
            // =====================================================

            if (request.getStatus() != null &&
                    !"PENDING".equalsIgnoreCase(
                            request.getStatus()
                    )) {

                session.setAttribute(
                        "kycUpdateError",
                        "This request has already been processed."
                );

                return "redirect:/kyc-update-form";
            }


            // =====================================================
            // 3. VALIDATE UPDATE FIELD
            // =====================================================

            if (updateField == null ||
                    updateField.trim().isEmpty()) {

                session.setAttribute(
                        "kycUpdateError",
                        "Please select the information you want to update."
                );

                return "redirect:/kyc-update-form";
            }


            updateField =
                    updateField
                            .trim()
                            .toUpperCase();


            if (!isSupportedField(updateField)) {

                session.setAttribute(
                        "kycUpdateError",
                        "Unsupported update field."
                );

                return "redirect:/kyc-update-form";
            }


            // =====================================================
            // 4. FIND CUSTOMER
            // =====================================================

            Customer customer =
                    customerRepository
                            .findByCustomerId(customerId)
                            .orElse(null);


            if (customer == null) {

                session.setAttribute(
                        "kycUpdateError",
                        "Customer information not found."
                );

                return "redirect:/kyc-update-form";
            }


            // =====================================================
            // 5. GET CURRENT VALUE FROM DATABASE
            // =====================================================

            String oldValue =
                    getCurrentValue(
                            customer,
                            updateField
                    );


            if (oldValue == null) {

                oldValue = "";
            }


            oldValue =
                    oldValue.trim();


            // =====================================================
            // 6. VALIDATE NEW VALUE
            // =====================================================

            if (newValue == null ||
                    newValue.trim().isEmpty()) {

                session.setAttribute(
                        "kycUpdateError",
                        "Please enter the new value."
                );

                return "redirect:/kyc-update-form";
            }


            newValue =
                    newValue.trim();


            // =====================================================
            // 7. NEW VALUE MUST BE DIFFERENT
            // =====================================================

            if (oldValue.equalsIgnoreCase(newValue)) {

                session.setAttribute(
                        "kycUpdateError",
                        "New value must be different from the current value."
                );

                return "redirect:/kyc-update-form";
            }


            // =====================================================
            // 8. MOBILE UPDATE VALIDATION
            // =====================================================

            if ("AADHAAR_MOBILE".equals(updateField)) {

                if (!newValue.matches(
                        "^[6-9][0-9]{9}$"
                )) {

                    session.setAttribute(
                            "kycUpdateError",
                            "Please enter a valid 10-digit Indian mobile number."
                    );

                    return "redirect:/kyc-update-form";
                }
            }


            // =====================================================
            // 9. OTHER KYC FIELDS REQUIRE PROOF
            // =====================================================

            else {

                if (proofType == null ||
                        proofType.trim().isEmpty()) {

                    session.setAttribute(
                            "kycUpdateError",
                            "Please select the proof type."
                    );

                    return "redirect:/kyc-update-form";
                }


                proofType =
                        proofType.trim();


                if (proofFile == null ||
                        proofFile.isEmpty()) {

                    session.setAttribute(
                            "kycUpdateError",
                            "Please upload the selected proof document."
                    );

                    return "redirect:/kyc-update-form";
                }


                String originalFilename =
                        proofFile.getOriginalFilename();


                if (originalFilename == null ||
                        !originalFilename
                                .toLowerCase()
                                .endsWith(".pdf")) {

                    session.setAttribute(
                            "kycUpdateError",
                            "Please upload a PDF proof document."
                    );

                    return "redirect:/kyc-update-form";
                }
            }


            // =====================================================
            // 10. PREVENT SAME PENDING REQUEST
            //
            // BUT IGNORE THE CURRENT REQUEST ID BECAUSE
            // WE ARE UPDATING THAT SAME REQUEST.
            // =====================================================

            List<CustomerRequest> existingRequests =
                    customerRequestRepository
                            .findByCustomerId(customerId);


            if (existingRequests != null) {

                for (CustomerRequest existingRequest :
                        existingRequests) {

                    if (existingRequest == null) {
                        continue;
                    }


                    // IMPORTANT:
                    // Do not compare the request against itself.
                    if (existingRequest.getId() != null &&
                            existingRequest.getId()
                                    .equals(requestId)) {

                        continue;
                    }


                    if (!"PENDING".equalsIgnoreCase(
                            existingRequest.getStatus()
                    )) {

                        continue;
                    }


                    String existingField =
                            existingRequest.getFieldName();


                    String existingOldValue =
                            existingRequest.getOldValue();


                    String existingNewValue =
                            existingRequest.getNewValue();


                    if (existingField == null ||
                            existingOldValue == null ||
                            existingNewValue == null) {

                        continue;
                    }


                    boolean sameField =
                            existingField
                                    .trim()
                                    .equalsIgnoreCase(
                                            updateField
                                    );


                    boolean sameOldValue =
                            existingOldValue
                                    .trim()
                                    .equalsIgnoreCase(
                                            oldValue
                                    );


                    boolean sameNewValue =
                            existingNewValue
                                    .trim()
                                    .equalsIgnoreCase(
                                            newValue
                                    );


                    if (sameField &&
                            sameOldValue &&
                            sameNewValue) {

                        session.setAttribute(
                                "kycUpdateError",
                                "You already have a PENDING request for this information. Please wait for Admin verification."
                        );

                        return "redirect:/kyc-update-form";
                    }
                }
            }


            // =====================================================
            // 11. UPDATE THE SAME REQUEST
            // =====================================================

            // DO NOT USE:
            //
            // CustomerRequest request =
            //         new CustomerRequest();
            //
            // The request was already found using requestId.


            request.setCustomerId(
                    customerId
            );


            request.setRequestType(
                    "KYC_UPDATE"
            );


            request.setFieldName(
                    updateField
            );


            request.setOldValue(
                    oldValue
            );


            request.setNewValue(
                    newValue
            );


            request.setReason(
                    reason != null
                            ? reason.trim()
                            : ""
            );


            String message =
                    additionalMessage != null &&
                    !additionalMessage.trim().isEmpty()

                            ? additionalMessage.trim()

                            : "Customer requested KYC information update for "
                              + updateField;


            request.setMessage(
                    message
            );


            // =====================================================
            // 12. MOBILE = OTP VERIFICATION
            // =====================================================

            if ("AADHAAR_MOBILE".equals(updateField)) {

                request.setVerificationType(
                        "OTP VERIFICATION"
                );


                request.setVerificationStatus(
                        "VERIFIED"
                );


                request.setProofFile(
                        null
                );
            }


            // =====================================================
            // 13. OTHER FIELDS = PROOF VERIFICATION
            // =====================================================

            else {

                request.setVerificationType(
                        proofType
                );


                request.setVerificationStatus(
                        "PENDING"
                );


                // =================================================
                // SAVE PROOF PDF
                // =================================================

                String originalFilename =
                        proofFile.getOriginalFilename();


                String safeFilename =
                        Paths.get(originalFilename)
                                .getFileName()
                                .toString();


                String uniqueFilename =
                        customerId
                        + "_"
                        + System.currentTimeMillis()
                        + "_"
                        + safeFilename;


                Path uploadDirectory =
                        Paths.get(
                                PROOF_DIRECTORY
                        );


                Files.createDirectories(
                        uploadDirectory
                );


                Path filePath =
                        uploadDirectory.resolve(
                                uniqueFilename
                        );


                Files.write(
                        filePath,
                        proofFile.getBytes()
                );


                request.setProofFile(
                        uniqueFilename
                );
            }


            // =====================================================
            // 14. STATUS
            // =====================================================

            request.setStatus(
                    "PENDING"
            );


            request.setRequestDate(
                    LocalDateTime.now()
            );


            // =====================================================
            // 15. UPDATE SAME REQUEST
            // =====================================================

            customerRequestRepository.save(
                    request
            );


            // =====================================================
            // 16. SUCCESS
            // =====================================================

            session.setAttribute(
                    "kycUpdateSuccess",
                    "KYC update request submitted successfully. Waiting for admin approval."
            );


            session.removeAttribute(
                    "kycUpdateError"
            );


            // Request has now been completed.
            session.removeAttribute(
                    "kycRequestId"
            );


            // =====================================================
            // 17. REDIRECT
            // =====================================================

            return "redirect:/customer-dashboard";


        } catch (Exception e) {

            e.printStackTrace();


            session.setAttribute(
                    "kycUpdateError",
                    "Unable to submit KYC update request. Please try again."
            );


            return "redirect:/kyc-update-form";
        }
    }


    // =========================================================
    // SUPPORTED FIELDS
    // =========================================================

    private boolean isSupportedField(
            String updateField) {

        return
                "AADHAAR_NAME".equals(updateField)
                ||
                "AADHAAR_DOB".equals(updateField)
                ||
                "AADHAAR_MOBILE".equals(updateField)
                ||
                "AADHAAR_ADDRESS".equals(updateField)
                ||
                "PAN_NAME".equals(updateField)
                ||
                "PAN_DOB".equals(updateField);
    }


    // =========================================================
    // GET CURRENT CUSTOMER VALUE
    // =========================================================

    private String getCurrentValue(
            Customer customer,
            String updateField) {

        switch (updateField) {

            case "AADHAAR_NAME":

                return customer.getFullName();


            case "PAN_NAME":

                return customer.getFullName();


            case "AADHAAR_DOB":

                return customer.getDateOfBirth() != null
                        ? customer.getDateOfBirth().toString()
                        : "";


            case "PAN_DOB":

                return customer.getDateOfBirth() != null
                        ? customer.getDateOfBirth().toString()
                        : "";


            case "AADHAAR_MOBILE":

                return customer.getMobile();


            case "AADHAAR_ADDRESS":

                return customer.getAddress();


            default:

                return "";
        }
    }
}