package com.ekyc.management.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ekyc.management.entity.EkycDocumentVersion;
import com.ekyc.management.model.Customer;
import com.ekyc.management.model.CustomerRequest;
import com.ekyc.management.model.Ekyc;
import com.ekyc.management.model.KycHistory;
import com.ekyc.management.repository.CustomerRepository;
import com.ekyc.management.repository.CustomerRequestRepository;
import com.ekyc.management.repository.EkycDocumentVersionRepository;
import com.ekyc.management.repository.EkycRepository;
import com.ekyc.management.repository.KycHistoryRepository;

@Controller
public class AdminRequestController {

    // ============================================================
    // REPOSITORIES
    // ============================================================

    @Autowired
    private CustomerRequestRepository customerRequestRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private KycHistoryRepository kycHistoryRepository;

    @Autowired
    private EkycRepository ekycRepository;

    @Autowired
    private EkycDocumentVersionRepository
            ekycDocumentVersionRepository;


    // ============================================================
    // ADMIN REQUEST LIST
    //
    // GET /admin/requests
    //
    // READ ONLY
    // ============================================================

    @GetMapping("/admin/requests")
    public String showRequests(
            Model model) {

        /*
         * IMPORTANT:
         *
         * Earlier this page only showed PENDING requests.
         *
         * That is not suitable anymore because the customer
         * KYC update is automatically completed after successful
         * face verification.
         *
         * Therefore, admin should see ALL requests.
         */

        List<CustomerRequest> requests =
                customerRequestRepository.findAll();


        model.addAttribute(
                "requests",
                requests
        );


        return "admin-requests";
    }


    // ============================================================
    // VIEW REQUEST DETAILS
    //
    // GET /admin/request-details?id=123
    //
    // READ ONLY
    // ============================================================

    @GetMapping("/admin/request-details")
    public String showRequestDetails(
            @RequestParam("id") Long id,
            Model model) {

        // ========================================================
        // FIND REQUEST
        // ========================================================

        CustomerRequest request =
                customerRequestRepository
                        .findById(id)
                        .orElse(null);


        if (request == null) {

            model.addAttribute(
                    "error",
                    "KYC request not found."
            );


            /*
             * Return to admin request list.
             */

            List<CustomerRequest> requests =
                    customerRequestRepository.findAll();


            model.addAttribute(
                    "requests",
                    requests
            );


            return "admin-requests";
        }


        // ========================================================
        // FIND CUSTOMER
        // ========================================================

        Customer customer =
                customerRepository
                        .findByCustomerId(
                                request.getCustomerId()
                        )
                        .orElse(null);


        // ========================================================
        // CUSTOMER KYC HISTORY
        // ========================================================

        List<KycHistory> histories =
                kycHistoryRepository
                        .findByCustomerIdOrderByChangedAtDesc(
                                request.getCustomerId()
                        );


        // ========================================================
        // GET SIGNED PDF VERSIONS
        // ========================================================

        List<EkycDocumentVersion> documentVersions =
                getDocumentVersions(
                        request.getCustomerId()
                );


        // ========================================================
        // SEND DATA TO THYMELEAF
        // ========================================================

        model.addAttribute(
                "request",
                request
        );


        model.addAttribute(
                "customer",
                customer
        );


        model.addAttribute(
                "histories",
                histories
        );


        model.addAttribute(
                "documentVersions",
                documentVersions
        );


        return "admin-request-details";
    }


    // ============================================================
    // ADMIN VIEW SIGNED PDF
    //
    // GET /admin/kyc/download-signed?id=123
    //
    // READ ONLY
    //
    // This does NOT generate or sign a PDF again.
    //
    // It only opens the already digitally signed PDF that was
    // previously stored in generated-documents.
    // ============================================================

    @GetMapping("/admin/kyc/download-signed")
    public ResponseEntity<Resource> downloadSignedKycPdf(
            @RequestParam("id") Long documentVersionId) {

        try {

            // ====================================================
            // FIND DOCUMENT VERSION
            // ====================================================

            EkycDocumentVersion documentVersion =
                    ekycDocumentVersionRepository
                            .findById(documentVersionId)
                            .orElse(null);


            if (documentVersion == null) {

                System.out.println(
                        "Document version not found: "
                                + documentVersionId
                );

                return ResponseEntity
                        .notFound()
                        .build();
            }


            // ====================================================
            // CHECK SIGNATURE STATUS
            // ====================================================

            if (!"SIGNED".equalsIgnoreCase(
                    documentVersion.getSignatureStatus()
            )) {

                System.out.println(
                        "Document is not signed. Version ID: "
                                + documentVersionId
                );

                return ResponseEntity
                        .notFound()
                        .build();
            }


            // ====================================================
            // GET STORED FILE PATH
            // ====================================================

            String storedPath =
                    documentVersion.getFilePath();


            if (storedPath == null
                    || storedPath.trim().isEmpty()) {

                System.out.println(
                        "Signed PDF file path is empty. Version ID: "
                                + documentVersionId
                );

                return ResponseEntity
                        .notFound()
                        .build();
            }


            // ====================================================
            // BASE DIRECTORY
            //
            // Your existing signing code stores the signed PDF
            // inside:
            //
            // generated-documents/
            // ====================================================

            Path baseDirectory =
                    Paths.get(
                            "generated-documents"
                    )
                    .toAbsolutePath()
                    .normalize();


            // ====================================================
            // RESOLVE PDF PATH
            // ====================================================

            Path pdfPath =
                    Paths.get(
                            storedPath
                    )
                    .toAbsolutePath()
                    .normalize();


            // ====================================================
            // SECURITY CHECK
            //
            // Make sure the requested file is located inside
            // generated-documents.
            // ====================================================

            if (!pdfPath.startsWith(
                    baseDirectory
            )) {

                System.out.println(
                        "Unauthorized PDF path: "
                                + pdfPath
                );

                return ResponseEntity
                        .status(403)
                        .build();
            }


            // ====================================================
            // CHECK FILE EXISTS
            // ====================================================

            if (!Files.exists(pdfPath)
                    || !Files.isRegularFile(pdfPath)) {

                System.out.println(
                        "Signed PDF file not found: "
                                + pdfPath
                );

                return ResponseEntity
                        .notFound()
                        .build();
            }


            // ====================================================
            // CREATE RESOURCE
            // ====================================================

            Resource resource =
                    new FileSystemResource(
                            pdfPath.toFile()
                    );


            // ====================================================
            // GET FILE NAME
            // ====================================================

            String fileName =
                    documentVersion.getFileName();


            if (fileName == null
                    || fileName.trim().isEmpty()) {

                fileName =
                        pdfPath
                                .getFileName()
                                .toString();
            }


            // ====================================================
            // RETURN PDF
            //
            // "inline" means the browser/Adobe can open the PDF
            // instead of forcing a download.
            // ====================================================

            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.APPLICATION_PDF
                    )
                    .contentLength(
                            Files.size(pdfPath)
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\""
                                    + fileName
                                    + "\""
                    )
                    .body(resource);


        } catch (IOException e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .build();

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }


    // ============================================================
    // GET CUSTOMER eKYC DOCUMENT VERSIONS
    // ============================================================

    private List<EkycDocumentVersion> getDocumentVersions(
            String customerId) {

        /*
         * Your EkycRepository returns List<Ekyc>.
         *
         * Therefore we do NOT use:
         *
         *     .orElse(null)
         *
         * here.
         */

        List<Ekyc> ekycList =
                ekycRepository
                        .findByCustomerIdOrderBySubmittedAtDesc(
                                customerId
                        );


        if (ekycList == null ||
                ekycList.isEmpty()) {

            return List.of();
        }


        // ========================================================
        // GET LATEST EKYC RECORD
        // ========================================================

        Ekyc ekyc =
                ekycList.get(0);


        if (ekyc == null ||
                ekyc.getId() == null) {

            return List.of();
        }


        // ========================================================
        // CURRENT REPOSITORY METHOD
        //
        // Gets latest PDF version.
        // ========================================================

        Optional<EkycDocumentVersion>
                latestVersion =
                ekycDocumentVersionRepository
                        .findTopByEkycIdOrderByVersionNumberDesc(
                                ekyc.getId()
                        );


        if (latestVersion.isEmpty()) {

            return List.of();
        }


        return List.of(
                latestVersion.get()
        );
    }
}