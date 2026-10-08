package com.ekyc.management.controller;

import com.ekyc.management.entity.DigitalSignature;
import com.ekyc.management.repository.DigitalSignatureRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/verify")
public class VerificationController {

    private final DigitalSignatureRepository
            digitalSignatureRepository;

    public VerificationController(
            DigitalSignatureRepository
                    digitalSignatureRepository) {

        this.digitalSignatureRepository =
                digitalSignatureRepository;
    }


    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>>
            verifySignature(
                    @PathVariable String token) {

        Optional<DigitalSignature> signature =
                digitalSignatureRepository
                        .findByVerificationToken(token);

        Map<String, Object> response =
                new HashMap<>();

        if (signature.isEmpty()) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Digital signature not found"
            );

            response.put(
                    "status",
                    "INVALID"
            );

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(response);
        }

        DigitalSignature digitalSignature =
                signature.get();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "Digital signature verified"
        );

        response.put(
                "status",
                digitalSignature.getStatus()
        );

        response.put(
                "customerName",
                digitalSignature.getCustomerName()
        );

        response.put(
                "certificateNumber",
                digitalSignature
                        .getCertificateNumber()
        );

        response.put(
                "issuer",
                digitalSignature.getIssuer()
        );

        response.put(
                "signedDate",
                digitalSignature.getSignedDate()
        );

        response.put(
                "verificationToken",
                digitalSignature
                        .getVerificationToken()
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/test")
    public ResponseEntity<String> test() {

        return ResponseEntity.ok(
                "Digital Signature Verification API is working!"
        );
    }
}