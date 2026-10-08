package com.ekyc.management.controller;

import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ekyc.management.service.SoftHSMKeyService;

@RestController
public class SoftHSMTestController {

    private final SoftHSMKeyService softHSMKeyService;

    public SoftHSMTestController(
            SoftHSMKeyService softHSMKeyService) {
        this.softHSMKeyService = softHSMKeyService;
    }

    @GetMapping("/api/softhsm/test")
    public String testSoftHSM() {

        try {

            PrivateKey privateKey =
                    softHSMKeyService.getPrivateKey();

            Certificate certificate =
                    softHSMKeyService.getCertificate();

            return """
                    SoftHSM connection successful.

                    Alias: %s
                    Private Key Algorithm: %s
                    Private Key Format: %s
                    Certificate Type: %s
                    Public Key Algorithm: %s
                    """.formatted(
                            softHSMKeyService.getAlias(),
                            privateKey.getAlgorithm(),
                            privateKey.getFormat(),
                            certificate.getType(),
                            certificate.getPublicKey().getAlgorithm()
                    );

        } catch (Exception e) {

            e.printStackTrace();

            return "SoftHSM connection failed: "
                    + e.getMessage();
        }
    }
}