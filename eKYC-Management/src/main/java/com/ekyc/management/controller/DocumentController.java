package com.ekyc.management.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DocumentController {

    private static final String DOCUMENT_FOLDER =
            "generated-documents";


    // =====================================================
    // DOWNLOAD SIGNED KYC PDF
    // GET /download-kyc/{customerId}
    // =====================================================

    @GetMapping("/download-kyc/{customerId}")
    public ResponseEntity<Resource> downloadKycPdf(
            @PathVariable String customerId) {

        // =============================================
        // CREATE SIGNED PDF FILE PATH
        // =============================================

        Path filePath = Paths.get(
                DOCUMENT_FOLDER,
                customerId + "-signed-kyc.pdf"
        );


        // =============================================
        // CHECK WHETHER SIGNED PDF EXISTS
        // =============================================

        if (!Files.exists(filePath)) {

            return ResponseEntity
                    .notFound()
                    .build();
        }


        // =============================================
        // LOAD SIGNED PDF
        // =============================================

        Resource resource =
                new FileSystemResource(filePath);


        // =============================================
        // RETURN SIGNED PDF FOR DOWNLOAD
        // =============================================

        return ResponseEntity.ok()

                .contentType(
                        MediaType.APPLICATION_PDF
                )

                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                        + customerId
                        + "-signed-kyc.pdf\""
                )

                .body(resource);
    }
}