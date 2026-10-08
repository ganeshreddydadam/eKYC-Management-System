package com.ekyc.management.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ekyc.management.model.CustomerRequest;
import com.ekyc.management.repository.CustomerRequestRepository;

@Controller
public class ProofFileController {

    @Autowired
    private CustomerRequestRepository customerRequestRepository;

    // =====================================================
    // OPEN PROOF FILE
    // GET /admin/proof/{id}
    // =====================================================

    @GetMapping("/admin/proof/{id}")
    public ResponseEntity<ByteArrayResource> openProofFile(
            @PathVariable("id") Long id) {

        // =================================================
        // FIND CUSTOMER REQUEST
        // =================================================

        CustomerRequest request =
                customerRequestRepository
                        .findById(id)
                        .orElse(null);

        if (request == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        // =================================================
        // GET STORED PROOF FILE
        // =================================================

        String proofFile =
                request.getProofFile();

        if (proofFile == null ||
                proofFile.trim().isEmpty()) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        try {

            // =================================================
            // PROOF FILE LOCATION
            // =================================================
            //
            // The uploaded proof files are stored in:
            //
            // uploads/proofs/
            //
            // proofFile normally contains the generated
            // filename, for example:
            //
            // abc123.pdf
            //
            // =================================================

            Path proofDirectory =
                    Paths.get(
                            "uploads",
                            "proofs"
                    )
                    .toAbsolutePath()
                    .normalize();

            // Remove any accidental path information
            // and use only the filename stored in DB.
            String fileName =
                    Paths.get(proofFile)
                            .getFileName()
                            .toString();

            Path path =
                    proofDirectory
                            .resolve(fileName)
                            .normalize();

            // =================================================
            // SECURITY CHECK
            // =================================================

            if (!path.startsWith(proofDirectory)) {

                return ResponseEntity
                        .badRequest()
                        .build();
            }

            // =================================================
            // CHECK FILE EXISTS
            // =================================================

            if (!Files.exists(path) ||
                    !Files.isRegularFile(path)) {

                System.out.println(
                        "Proof file not found: "
                                + path
                );

                return ResponseEntity
                        .notFound()
                        .build();
            }

            // =================================================
            // READ PDF
            // =================================================

            byte[] fileBytes =
                    Files.readAllBytes(path);

            ByteArrayResource resource =
                    new ByteArrayResource(
                            fileBytes
                    );

            // =================================================
            // FILE NAME
            // =================================================

            String actualFileName =
                    path.getFileName()
                            .toString();

            // =================================================
            // RETURN PDF
            // =================================================

            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.APPLICATION_PDF
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\""
                                    + actualFileName
                                    + "\""
                    )
                    .contentLength(
                            fileBytes.length
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
}