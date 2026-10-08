package com.ekyc.management.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ekyc.management.model.Customer;
import com.ekyc.management.model.CustomerRequest;
import com.ekyc.management.repository.CustomerRepository;
import com.ekyc.management.repository.CustomerRequestRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class KycUpdateController {

    private final CustomerRepository customerRepository;
    private final CustomerRequestRepository customerRequestRepository;

    private static final String PROOF_DIRECTORY = "uploads/proofs";

    public KycUpdateController(
            CustomerRepository customerRepository,
            CustomerRequestRepository customerRequestRepository) {

        this.customerRepository = customerRepository;
        this.customerRequestRepository = customerRequestRepository;
    }

    // ============================================================
    // SUBMIT KYC UPDATE
    // ============================================================

    @PostMapping("/submit-kyc-update")
    public String submitKycUpdate(

            @RequestParam(value = "requestId", required = false)
            Long requestId,

            @RequestParam(value = "fieldName", required = false)
            String fieldName,

            @RequestParam(value = "newValue", required = false)
            String newValue,

            @RequestParam(value = "reason", required = false)
            String reason,

            @RequestParam(value = "message", required = false)
            String message,

            @RequestParam(value = "verificationType", required = false)
            String verificationType,

            @RequestParam(value = "proofFile", required = false)
            MultipartFile proofFile,

            HttpSession session) {

        try {

            // ====================================================
            // 1. GET LOGGED-IN CUSTOMER ID
            // ====================================================

            String customerId =
                    (String) session.getAttribute(
                            "loggedInCustomerId");

            if (customerId == null
                    || customerId.trim().isEmpty()) {

                session.setAttribute(
                        "kycUpdateError",
                        "Session expired. Please login again.");

                return "redirect:/login";
            }

            customerId = customerId.trim();

            // ====================================================
            // 2. GET CUSTOMER
            // ====================================================

            /*
             * IMPORTANT:
             * findByCustomerId() returns Optional<Customer>.
             *
             * Therefore .orElse(null) is required.
             */
            Customer customer =
                    customerRepository
                            .findByCustomerId(customerId)
                            .orElse(null);

            if (customer == null) {

                session.setAttribute(
                        "kycUpdateError",
                        "Customer account not found.");

                return "redirect:/customer-dashboard";
            }

            // ====================================================
            // 3. CLEAN VALUES
            // ====================================================

            if (fieldName != null) {
                fieldName = fieldName.trim();
            }

            if (newValue != null) {
                newValue = newValue.trim();
            }

            if (reason != null) {
                reason = reason.trim();
            }

            if (message != null) {
                message = message.trim();
            }

            if (verificationType != null) {
                verificationType =
                        verificationType.trim();
            }

            // ====================================================
            // 4. VALIDATE FIELD
            // ====================================================

            if (fieldName == null
                    || fieldName.isEmpty()) {

                session.setAttribute(
                        "kycUpdateError",
                        "Please select the information to update.");

                return "redirect:/kyc-update-form";
            }

            if (!isSupportedField(fieldName)) {

                session.setAttribute(
                        "kycUpdateError",
                        "Invalid KYC update field.");

                return "redirect:/kyc-update-form";
            }

            // ====================================================
            // 5. FIND CUSTOMER REQUEST
            // ====================================================

            CustomerRequest request = null;

            // First try requestId from form
            if (requestId != null) {

                request =
                        customerRequestRepository
                                .findById(requestId)
                                .orElse(null);
            }

            // If not found, use session request ID
            if (request == null) {

                Object sessionRequestId =
                        session.getAttribute(
                                "kycRequestId");

                if (sessionRequestId != null) {

                    try {

                        Long id =
                                Long.parseLong(
                                        sessionRequestId.toString());

                        request =
                                customerRequestRepository
                                        .findById(id)
                                        .orElse(null);

                    } catch (NumberFormatException e) {

                        request = null;
                    }
                }
            }

            // ====================================================
            // 6. FINAL FALLBACK
            // ====================================================

            if (request == null) {

                List<CustomerRequest> requests =
                        customerRequestRepository
                                .findByCustomerIdOrderByRequestDateDesc(
                                        customerId);

                if (requests != null) {

                    for (CustomerRequest r : requests) {

                        if (r == null) {
                            continue;
                        }

                        if (!customerId.equals(
                                r.getCustomerId())) {
                            continue;
                        }

                        String status =
                                r.getStatus();

                        if ("PENDING".equalsIgnoreCase(status)
                                || "PROCESSING"
                                        .equalsIgnoreCase(status)) {

                            request = r;
                            break;
                        }
                    }
                }
            }

            // ====================================================
            // 7. REQUEST NOT FOUND
            // ====================================================

            if (request == null) {

                session.setAttribute(
                        "kycUpdateError",
                        "KYC update request not found. Please start a new request.");

                return "redirect:/kyc-update-form";
            }

            // ====================================================
            // 8. SECURITY CHECK
            // ====================================================

            if (request.getCustomerId() == null
                    || !customerId.equals(
                            request.getCustomerId())) {

                session.setAttribute(
                        "kycUpdateError",
                        "You are not authorized to process this request.");

                return "redirect:/customer-dashboard";
            }

            // ====================================================
            // 9. GET OLD VALUE
            // ====================================================

            String oldValue =
                    getCurrentValue(
                            customer,
                            fieldName);

            // ====================================================
            // 10. MOBILE UPDATE
            // ====================================================

            if ("AADHAAR_MOBILE".equals(fieldName)) {

                if (newValue == null
                        || !newValue.matches(
                                "^[6-9][0-9]{9}$")) {

                    session.setAttribute(
                            "kycUpdateError",
                            "Invalid mobile number.");

                    return "redirect:/kyc-update-form";
                }

                /*
                 * OTP has already been verified by
                 * the JavaScript in your form.
                 */

                customer.setMobile(newValue);

                customerRepository.save(customer);

                request.setCustomerId(customerId);
                request.setRequestType("KYC_UPDATE");
                request.setFieldName(fieldName);
                request.setOldValue(oldValue);
                request.setNewValue(newValue);
                request.setReason(reason);
                request.setMessage(message);

                request.setVerificationType(
                        "OTP VERIFICATION");

                request.setVerificationStatus(
                        "VERIFIED");

                request.setStatus(
                        "COMPLETED");

                request.setCompletedDate(
                        LocalDateTime.now());

                if (request.getRequestDate() == null) {

                    request.setRequestDate(
                            LocalDateTime.now());
                }

                customerRequestRepository.save(request);

                session.setAttribute(
                        "completedKycRequestId",
                        request.getId());

                session.setAttribute(
                        "kycUpdateSuccess",
                        "Mobile number updated successfully.");

                return "redirect:/kyc-update-success";
            }

            // ====================================================
            // 11. OTHER KYC FIELDS REQUIRE PROOF
            // ====================================================

            if (newValue == null
                    || newValue.trim().isEmpty()) {

                session.setAttribute(
                        "kycUpdateError",
                        "Please enter the new value.");

                return "redirect:/kyc-update-form";
            }

            if (verificationType == null
                    || verificationType.isEmpty()) {

                session.setAttribute(
                        "kycUpdateError",
                        "Please select proof type.");

                return "redirect:/kyc-update-form";
            }

            if (proofFile == null
                    || proofFile.isEmpty()) {

                session.setAttribute(
                        "kycUpdateError",
                        "Please upload the proof document.");

                return "redirect:/kyc-update-form";
            }

            // ====================================================
            // 12. FILE SIZE
            // ====================================================

            long maxSize =
                    10L * 1024L * 1024L;

            if (proofFile.getSize() > maxSize) {

                session.setAttribute(
                        "kycUpdateError",
                        "File size must be below 10 MB.");

                return "redirect:/kyc-update-form";
            }

            // ====================================================
            // 13. FILE NAME
            // ====================================================

            String originalFileName =
                    proofFile.getOriginalFilename();

            if (originalFileName == null
                    || originalFileName.trim().isEmpty()) {

                session.setAttribute(
                        "kycUpdateError",
                        "Invalid proof file.");

                return "redirect:/kyc-update-form";
            }

            originalFileName =
                    Paths.get(originalFileName)
                            .getFileName()
                            .toString();

            String lowerName =
                    originalFileName.toLowerCase();

            // ====================================================
            // 14. ALLOWED FILE TYPES
            // ====================================================

            if (!lowerName.endsWith(".pdf")
                    && !lowerName.endsWith(".jpg")
                    && !lowerName.endsWith(".jpeg")
                    && !lowerName.endsWith(".png")) {

                session.setAttribute(
                        "kycUpdateError",
                        "Only PDF, JPG, JPEG and PNG files are allowed.");

                return "redirect:/kyc-update-form";
            }

            // ====================================================
            // 15. CREATE DIRECTORY
            // ====================================================

            Path directory =
                    Paths.get(PROOF_DIRECTORY);

            if (!Files.exists(directory)) {

                Files.createDirectories(directory);
            }

            // ====================================================
            // 16. EXTENSION
            // ====================================================

            String extension = "";

            int dot =
                    originalFileName.lastIndexOf('.');

            if (dot >= 0) {

                extension =
                        originalFileName
                                .substring(dot)
                                .toLowerCase();
            }

            // ====================================================
            // 17. UNIQUE FILE NAME
            // ====================================================

            String uniqueFileName =
                    customerId
                    + "_request_"
                    + request.getId()
                    + "_"
                    + UUID.randomUUID()
                    + extension;

            Path target =
                    directory.resolve(
                            uniqueFileName);

            // ====================================================
            // 18. SAVE FILE
            // ====================================================

            Files.copy(
                    proofFile.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING);

            // ====================================================
            // 19. SAVE REQUEST
            // ====================================================

            request.setCustomerId(customerId);

            request.setRequestType(
                    "KYC_UPDATE");

            request.setFieldName(
                    fieldName);

            request.setOldValue(
                    oldValue);

            request.setNewValue(
                    newValue);

            request.setReason(
                    reason);

            request.setMessage(
                    message);

            request.setVerificationType(
                    verificationType);

            request.setProofFile(
                    uniqueFileName);

            request.setProofPhoto(null);

            request.setLiveFacePhoto(null);

            request.setFaceMatchScore(null);

            request.setVerificationStatus(
                    "PENDING");

            request.setStatus(
                    "PROCESSING");

            if (request.getRequestDate() == null) {

                request.setRequestDate(
                        LocalDateTime.now());
            }

            request.setCompletedDate(null);

            // ====================================================
            // 20. SAVE DATABASE
            // ====================================================

            CustomerRequest savedRequest =
                    customerRequestRepository.save(
                            request);

            // ====================================================
            // 21. SAVE SESSION REQUEST ID
            // ====================================================

            session.setAttribute(
                    "kycRequestId",
                    savedRequest.getId());

            session.setAttribute(
                    "faceVerificationRequestId",
                    savedRequest.getId());

            // ====================================================
            // 22. GO TO FACE VERIFICATION
            // ====================================================

            session.setAttribute(
                    "kycUpdateSuccess",
                    "Proof uploaded successfully. Please complete face verification.");

            return "redirect:/face-verification";

        } catch (IOException e) {

            e.printStackTrace();

            session.setAttribute(
                    "kycUpdateError",
                    "Unable to save proof document.");

            return "redirect:/kyc-update-form";

        } catch (Exception e) {

            e.printStackTrace();

            session.setAttribute(
                    "kycUpdateError",
                    "KYC update failed: "
                            + e.getMessage());

            return "redirect:/kyc-update-form";
        }
    }

    // ============================================================
    // SUPPORTED FIELDS
    // ============================================================

    private boolean isSupportedField(
            String fieldName) {

        return
                "AADHAAR_NAME".equals(fieldName)
                || "AADHAAR_DOB".equals(fieldName)
                || "AADHAAR_MOBILE".equals(fieldName)
                || "AADHAAR_ADDRESS".equals(fieldName)

                || "PAN_NAME".equals(fieldName)
                || "PAN_DOB".equals(fieldName)
                || "PAN_ADDRESS".equals(fieldName)

                || "DRIVING_LICENSE_NAME".equals(fieldName)
                || "DRIVING_LICENSE_DOB".equals(fieldName)
                || "DRIVING_LICENSE_MOBILE".equals(fieldName)
                || "DRIVING_LICENSE_ADDRESS".equals(fieldName)
                || "DRIVING_LICENSE_STATE".equals(fieldName)
                || "DRIVING_LICENSE_PINCODE".equals(fieldName);
    }

    // ============================================================
    // GET CURRENT VALUE
    // ============================================================

    private String getCurrentValue(
            Customer customer,
            String fieldName) {

        switch (fieldName) {

            case "AADHAAR_NAME":
            case "PAN_NAME":
            case "DRIVING_LICENSE_NAME":

                return customer.getFullName();

            case "AADHAAR_DOB":
            case "PAN_DOB":
            case "DRIVING_LICENSE_DOB":

                return customer.getDateOfBirth() == null
                        ? null
                        : customer.getDateOfBirth().toString();

            case "AADHAAR_MOBILE":
            case "DRIVING_LICENSE_MOBILE":

                return customer.getMobile();

            case "AADHAAR_ADDRESS":
            case "PAN_ADDRESS":
            case "DRIVING_LICENSE_ADDRESS":

                return customer.getAddress();

            case "DRIVING_LICENSE_STATE":

                return customer.getState();

            case "DRIVING_LICENSE_PINCODE":

                return customer.getPincode();

            default:

                return null;
        }
    }
}