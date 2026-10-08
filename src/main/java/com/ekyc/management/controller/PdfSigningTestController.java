package com.ekyc.management.controller;

import com.ekyc.management.service.PdfSigningService;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
@RequestMapping("/test")
public class PdfSigningTestController {

    private final PdfSigningService pdfSigningService;

    public PdfSigningTestController(
            PdfSigningService pdfSigningService) {

        this.pdfSigningService = pdfSigningService;
    }

    @GetMapping("/sign-pdf")
    public ResponseEntity<ByteArrayResource> testSigning()
            throws Exception {

        // 1. Load test PDF from resources
        InputStream inputStream =
                getClass()
                        .getClassLoader()
                        .getResourceAsStream(
                                "test/unsigned.pdf"
                        );

        if (inputStream == null) {
            throw new IllegalStateException(
                    "Test PDF not found: test/unsigned.pdf"
            );
        }

        // 2. Read PDF into byte array
        byte[] unsignedPdf =
                inputStream.readAllBytes();

        // 3. Send PDF to PdfSigningService
        byte[] signedPdf =
                pdfSigningService.signPdf(unsignedPdf);

        // 4. Return signed PDF
        ByteArrayResource resource =
                new ByteArrayResource(signedPdf);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=signed-ekyc-test.pdf"
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .contentLength(
                        signedPdf.length
                )
                .body(resource);
    }
}