package com.ekyc.management.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ekyc.management.entity.DigitalSignature;
import com.ekyc.management.entity.EkycDocumentVersion;
import com.ekyc.management.model.Customer;
import com.ekyc.management.model.CustomerRequest;
import com.ekyc.management.model.Ekyc;
import com.ekyc.management.repository.CustomerRepository;
import com.ekyc.management.repository.CustomerRequestRepository;
import com.ekyc.management.repository.DigitalSignatureRepository;
import com.ekyc.management.repository.EkycDocumentVersionRepository;
import com.ekyc.management.repository.EkycRepository;
import com.ekyc.management.service.PdfService;
import com.ekyc.management.service.PdfSigningService;

@Controller
public class AdminRequestController {

    @Autowired
    private CustomerRequestRepository customerRequestRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // =====================================================
    // PDF SERVICE
    // =====================================================

    @Autowired
    private PdfService pdfService;

    // =====================================================
    // DIGITAL PDF SIGNING SERVICE
    // =====================================================

    @Autowired
    private PdfSigningService pdfSigningService;

    // =====================================================
    // eKYC REPOSITORY
    // =====================================================

    @Autowired
    private EkycRepository ekycRepository;

    // =====================================================
    // DIGITAL SIGNATURE REPOSITORY
    // =====================================================

    @Autowired
    private DigitalSignatureRepository digitalSignatureRepository;

    // =====================================================
    // DOCUMENT VERSION REPOSITORY
    // =====================================================

    @Autowired
    private EkycDocumentVersionRepository ekycDocumentVersionRepository;


    // =====================================================
    // ADMIN REQUEST LIST
    // GET /admin/requests
    // =====================================================

    @GetMapping("/admin/requests")
    public String showPendingRequests(Model model) {

        List<CustomerRequest> requests =
                customerRequestRepository.findByStatus("PENDING");

        model.addAttribute("requests", requests);

        return "admin-requests";
    }


    // =====================================================
    // VIEW REQUEST DETAILS
    // GET /admin/request-details
    // =====================================================

    @GetMapping("/admin/request-details")
    public String showRequestDetails(
            @RequestParam("id") Long id,
            Model model) {

        CustomerRequest request =
                customerRequestRepository
                        .findById(id)
                        .orElse(null);

        if (request == null) {

            model.addAttribute(
                    "error",
                    "Request not found."
            );

            List<CustomerRequest> requests =
                    customerRequestRepository
                            .findByStatus("PENDING");

            model.addAttribute(
                    "requests",
                    requests
            );

            return "admin-requests";
        }


        Customer customer =
                customerRepository
                        .findByCustomerId(
                                request.getCustomerId()
                        )
                        .orElse(null);

        model.addAttribute(
                "request",
                request
        );

        model.addAttribute(
                "customer",
                customer
        );

        return "admin-request-details";
    }


    // =====================================================
    // APPROVE REQUEST
    // POST /admin/approve-request
    // =====================================================

    @PostMapping("/admin/approve-request")
    public String approveRequest(
            @RequestParam("id") Long id,
            @RequestParam(
                    value = "adminRemarks",
                    required = false
            )
            String adminRemarks,
            RedirectAttributes redirectAttributes) {

        try {

            CustomerRequest request =
                    customerRequestRepository
                            .findById(id)
                            .orElse(null);

            if (request == null) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Request not found."
                );

                return "redirect:/admin/requests";
            }


            if (!"PENDING".equalsIgnoreCase(
                    request.getStatus())) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "This request has already been processed."
                );

                return "redirect:/admin/requests";
            }


            Customer customer =
                    customerRepository
                            .findByCustomerId(
                                    request.getCustomerId()
                            )
                            .orElse(null);

            if (customer == null) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Customer not found."
                );

                return "redirect:/admin/requests";
            }


            String fieldName =
                    request.getFieldName();

            String newValue =
                    request.getNewValue();


            if (fieldName == null ||
                    fieldName.trim().isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Request field name is empty."
                );

                return "redirect:/admin/requests";
            }


            if (newValue == null ||
                    newValue.trim().isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "New value is empty."
                );

                return "redirect:/admin/requests";
            }


            fieldName =
                    fieldName.trim().toUpperCase();

            newValue =
                    newValue.trim();


            // =================================================
            // UPDATE CUSTOMER INFORMATION
            // =================================================

            switch (fieldName) {

                case "AADHAAR_NAME":

                case "PAN_NAME":

                    customer.setFullName(
                            newValue
                    );

                    break;


                case "AADHAAR_DOB":

                case "PAN_DOB":

                    try {

                        LocalDate dob =
                                LocalDate.parse(
                                        newValue
                                );

                        customer.setDateOfBirth(
                                dob
                        );

                    } catch (Exception e) {

                        redirectAttributes.addFlashAttribute(
                                "error",
                                "Invalid Date of Birth. Expected format: YYYY-MM-DD"
                        );

                        return "redirect:/admin/requests";
                    }

                    break;


                case "AADHAAR_MOBILE":

                    customer.setMobile(
                            newValue
                    );

                    break;


                case "AADHAAR_ADDRESS":

                    customer.setAddress(
                            newValue
                    );

                    break;


                default:

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "Unsupported update field: "
                                    + fieldName
                    );

                    return "redirect:/admin/requests";
            }


            // =================================================
            // SAVE CUSTOMER
            // =================================================

            customerRepository.save(
                    customer
            );


            // =================================================
            // UPDATE REQUEST
            // =================================================

            request.setStatus(
                    "APPROVED"
            );

            request.setVerificationStatus(
                    "APPROVED"
            );

            request.setAdminRemarks(
                    adminRemarks
            );

            customerRequestRepository.save(
                    request
            );


            // =================================================
            // GENERATE UPDATED KYC PDF
            // =================================================

            String generatedPdfPath =
                    pdfService.generateUpdatedKycPdf(
                            customer,
                            request
                    );


            // =================================================
            // DIGITAL SIGNING
            // =================================================

            Path unsignedPdfPath =
                    Paths.get(
                            generatedPdfPath
                    );

            if (!Files.exists(unsignedPdfPath)) {

                throw new IllegalStateException(
                        "Generated KYC PDF not found: "
                                + generatedPdfPath
                );
            }


            // Read generated PDF

            byte[] unsignedPdf =
                    Files.readAllBytes(
                            unsignedPdfPath
                    );


            // Sign generated PDF

            byte[] signedPdf =
                    pdfSigningService.signPdf(
                            unsignedPdf
                    );


            // =================================================
            // SAVE SIGNED PDF
            // =================================================

            String signedFileName =
                    customer.getCustomerId()
                            + "-signed-kyc.pdf";

            Path signedPdfPath =
                    Paths.get(
                            "generated-documents",
                            signedFileName
                    );


            Files.write(
                    signedPdfPath,
                    signedPdf
            );


            // =================================================
            // GET eKYC RECORD
            // =================================================

            List<Ekyc> ekycList =
                    ekycRepository
                            .findByCustomerIdOrderBySubmittedAtDesc(
                                    customer.getCustomerId()
                            );


            Long ekycId = null;

            if (!ekycList.isEmpty()) {

                ekycId =
                        ekycList.get(0).getId();
            }


            // =================================================
            // SAVE DOCUMENT VERSION
            // =================================================

            if (ekycId != null) {

                Optional<EkycDocumentVersion>
                        latestVersion =
                        ekycDocumentVersionRepository
                                .findTopByEkycIdOrderByVersionNumberDesc(
                                        ekycId
                                );


                int nextVersion = 1;

                if (latestVersion.isPresent() &&
                        latestVersion.get()
                                .getVersionNumber() != null) {

                    nextVersion =
                            latestVersion.get()
                                    .getVersionNumber()
                                    + 1;
                }


                // Calculate SHA-256 hash

                MessageDigest digest =
                        MessageDigest.getInstance(
                                "SHA-256"
                        );

                byte[] hashBytes =
                        digest.digest(
                                signedPdf
                        );

                StringBuilder hash =
                        new StringBuilder();

                for (byte b : hashBytes) {

                    hash.append(
                            String.format(
                                    "%02x",
                                    b
                            )
                    );
                }


                EkycDocumentVersion version =
                        new EkycDocumentVersion();

                version.setEkycId(
                        ekycId
                );

                version.setVersionNumber(
                        nextVersion
                );

                version.setFileName(
                        signedFileName
                );

                version.setFilePath(
                        signedPdfPath.toString()
                );

                version.setDocumentHash(
                        hash.toString()
                );

                version.setSignatureStatus(
                        "SIGNED"
                );

                version.setSignedAt(
                        LocalDateTime.now()
                );

                version.setCreatedAt(
                        LocalDateTime.now()
                );

                ekycDocumentVersionRepository.save(
                        version
                );


                // =================================================
                // SAVE DIGITAL SIGNATURE RECORD
                // =================================================

                DigitalSignature digitalSignature =
                        new DigitalSignature();

                digitalSignature.setEkycId(
                        ekycId
                );

                digitalSignature.setCustomerName(
                        customer.getFullName()
                );

                digitalSignature.setCertificateNumber(
                        "ekyc-signing"
                );

                digitalSignature.setIssuer(
                        "eKYC Demo Authority"
                );

                digitalSignature.setSignedDate(
                        LocalDateTime.now()
                );

                digitalSignature.setStatus(
                        "SIGNED"
                );

                digitalSignature.setVerificationToken(
                        java.util.UUID
                                .randomUUID()
                                .toString()
                );

                digitalSignatureRepository.save(
                        digitalSignature
                );
            }


            // =================================================
            // SUCCESS MESSAGE
            // =================================================

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Request approved successfully. Customer information has been updated, KYC PDF has been generated and digitally signed."
            );

            return "redirect:/admin/requests";


        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to approve request: "
                            + e.getMessage()
            );

            return "redirect:/admin/requests";
        }
    }


    // =====================================================
    // REJECT REQUEST
    // POST /admin/reject-request
    // =====================================================

    @PostMapping("/admin/reject-request")
    public String rejectRequest(
            @RequestParam("id") Long id,
            @RequestParam(
                    value = "adminRemarks",
                    required = false
            )
            String adminRemarks,
            RedirectAttributes redirectAttributes) {

        try {

            CustomerRequest request =
                    customerRequestRepository
                            .findById(id)
                            .orElse(null);

            if (request == null) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Request not found."
                );

                return "redirect:/admin/requests";
            }


            if (!"PENDING".equalsIgnoreCase(
                    request.getStatus())) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "This request has already been processed."
                );

                return "redirect:/admin/requests";
            }


            request.setStatus(
                    "REJECTED"
            );

            request.setVerificationStatus(
                    "REJECTED"
            );

            request.setAdminRemarks(
                    adminRemarks
            );

            customerRequestRepository.save(
                    request
            );


            redirectAttributes.addFlashAttribute(
                    "message",
                    "Request rejected successfully."
            );

            return "redirect:/admin/requests";


        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to reject request: "
                            + e.getMessage()
            );

            return "redirect:/admin/requests";
        }
    }
}