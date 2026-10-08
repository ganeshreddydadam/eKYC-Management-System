package com.ekyc.management.controller;

import java.io.IOException;

import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.Paths;

import java.nio.file.StandardOpenOption;

import java.security.MessageDigest;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.util.Base64;

import java.util.HexFormat;
import java.util.List;

import org.springframework.core.io.FileSystemResource;

import org.springframework.core.io.Resource;

import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;

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

import com.ekyc.management.service.EkycPdfService;

import com.ekyc.management.service.FaceVerificationService;

import com.ekyc.management.service.FaceVerificationService.FaceVerificationResult;

import com.ekyc.management.service.PdfSigningService;

import com.ekyc.management.service.ProofPhotoExtractionService;

import jakarta.servlet.http.HttpSession;

@Controller

public class FaceVerificationController {

    private final CustomerRepository customerRepository;

    private final CustomerRequestRepository customerRequestRepository;

    private final KycHistoryRepository kycHistoryRepository;

    private final EkycRepository ekycRepository;

    private final EkycDocumentVersionRepository ekycDocumentVersionRepository;

    private final FaceVerificationService faceVerificationService;

    private final ProofPhotoExtractionService proofPhotoExtractionService;

    private final EkycPdfService ekycPdfService;

    private final PdfSigningService pdfSigningService;

    private static final String LIVE_FACE_DIRECTORY =

            "uploads/live-faces";

    private static final String PROOF_DIRECTORY =

            "uploads/proofs";

    private static final String EKYC_DIRECTORY =

            "uploads/ekyc";

    public FaceVerificationController(

            CustomerRepository customerRepository,

            CustomerRequestRepository customerRequestRepository,

            KycHistoryRepository kycHistoryRepository,

            EkycRepository ekycRepository,

            EkycDocumentVersionRepository ekycDocumentVersionRepository,

            FaceVerificationService faceVerificationService,

            ProofPhotoExtractionService proofPhotoExtractionService,

            EkycPdfService ekycPdfService,

            PdfSigningService pdfSigningService) {

        this.customerRepository =

                customerRepository;

        this.customerRequestRepository =

                customerRequestRepository;

        this.kycHistoryRepository =

                kycHistoryRepository;

        this.ekycRepository =

                ekycRepository;

        this.ekycDocumentVersionRepository =

                ekycDocumentVersionRepository;

        this.faceVerificationService =

                faceVerificationService;

        this.proofPhotoExtractionService =

                proofPhotoExtractionService;

        this.ekycPdfService =

                ekycPdfService;

        this.pdfSigningService =

                pdfSigningService;

    }

    // ============================================================

    // SHOW FACE VERIFICATION PAGE

    // ============================================================

    @GetMapping("/face-verification")

    public String showFaceVerification(

            HttpSession session,

            Model model) {

        String customerId =

                (String) session.getAttribute(

                        "loggedInCustomerId"

                );

        if (customerId == null ||

                customerId.trim().isEmpty()) {

            return "redirect:/login";

        }

        Object requestIdObject =

                session.getAttribute(

                        "kycRequestId"

                );

        if (requestIdObject == null) {

            model.addAttribute(

                    "error",

                    "KYC update request was not found."

            );

            return "face-verification";

        }

        Long requestId;

        try {

            requestId =

                    Long.parseLong(

                            requestIdObject.toString()

                    );

        } catch (NumberFormatException e) {

            model.addAttribute(

                    "error",

                    "Invalid KYC request ID."

            );

            return "face-verification";

        }

        CustomerRequest request =

                customerRequestRepository

                        .findById(requestId)

                        .orElse(null);

        if (request == null) {

            model.addAttribute(

                    "error",

                    "KYC update request not found."

            );

            return "face-verification";

        }

        if (request.getCustomerId() == null ||

                !customerId.equals(

                        request.getCustomerId()

                )) {

            model.addAttribute(

                    "error",

                    "You are not authorized to access this request."

            );

            return "face-verification";

        }

        Customer customer =

                customerRepository

                        .findByCustomerId(customerId)

                        .orElse(null);

        if (customer == null) {

            return "redirect:/login";

        }

        model.addAttribute(

                "customer",

                customer

        );

        model.addAttribute(

                "request",

                request

        );

        return "face-verification";

    }

    // ============================================================

    // VERIFY FACE

    // ============================================================

    @PostMapping("/verify-face")

    public String verifyFace(

            @RequestParam("liveFace") String liveFace,

            HttpSession session,

            Model model) {

        String customerId =

                (String) session.getAttribute(

                        "loggedInCustomerId"

                );

        if (customerId == null ||

                customerId.trim().isEmpty()) {

            return "redirect:/login";

        }

        Object requestIdObject =

                session.getAttribute(

                        "kycRequestId"

                );

        if (requestIdObject == null) {

            model.addAttribute(

                    "error",

                    "KYC update request was not found."

            );

            return "face-verification";

        }

        Long requestId;

        try {

            requestId =

                    Long.parseLong(

                            requestIdObject.toString()

                    );

        } catch (NumberFormatException e) {

            model.addAttribute(

                    "error",

                    "Invalid KYC request ID."

            );

            return "face-verification";

        }

        CustomerRequest request =

                customerRequestRepository

                        .findById(requestId)

                        .orElse(null);

        if (request == null) {

            model.addAttribute(

                    "error",

                    "KYC update request not found."

            );

            return "face-verification";

        }

        // ========================================================

        // SECURITY CHECK

        // ========================================================

        if (request.getCustomerId() == null ||

                !customerId.equals(

                        request.getCustomerId()

                )) {

            model.addAttribute(

                    "error",

                    "You are not authorized to process this request."

            );

            return "face-verification";

        }

        // ========================================================

        // REQUEST STATUS CHECK

        // ========================================================

        if ("COMPLETED".equalsIgnoreCase(

                request.getStatus()

        )) {

            model.addAttribute(

                    "error",

                    "This KYC request has already been completed."

            );

            return "face-verification";

        }

        // ========================================================

        // LOAD CUSTOMER

        // ========================================================

        Customer customer =

                customerRepository

                        .findByCustomerId(customerId)

                        .orElse(null);

        if (customer == null) {

            return "redirect:/login";

        }

        // ========================================================

        // CHECK PROOF FILE

        // ========================================================

        String proofFile =

                request.getProofFile();

        if (proofFile == null ||

                proofFile.trim().isEmpty()) {

            return showError(

                    model,

                    customer,

                    request,

                    "Uploaded proof document was not found."

            );

        }

        Path proofDirectory =

                Paths.get(PROOF_DIRECTORY)

                        .toAbsolutePath()

                        .normalize();

        Path proofPath =

                proofDirectory

                        .resolve(

                                Paths.get(proofFile)

                                        .getFileName()

                                        .toString()

                        )

                        .normalize();

        if (!proofPath.startsWith(proofDirectory)) {

            return showError(

                    model,

                    customer,

                    request,

                    "Invalid proof file."

            );

        }

        if (!Files.exists(proofPath) ||

                !Files.isRegularFile(proofPath)) {

            return showError(

                    model,

                    customer,

                    request,

                    "Uploaded proof file does not exist."

            );

        }

        // ========================================================

        // CHECK LIVE CAMERA IMAGE

        // ========================================================

        if (liveFace == null ||

                liveFace.trim().isEmpty()) {

            return showError(

                    model,

                    customer,

                    request,

                    "Live face image was not received."

            );

        }

        try {

            // ====================================================

            // REMOVE DATA URL PREFIX

            // ====================================================

            String base64Image =

                    liveFace.trim();

            if (base64Image.contains(",")) {

                base64Image =

                        base64Image.substring(

                                base64Image.indexOf(",") + 1

                        );

            }

            if (base64Image.trim().isEmpty()) {

                return showError(

                        model,

                        customer,

                        request,

                        "Live camera image is empty."

                );

            }

            // ====================================================

            // DECODE CAMERA IMAGE

            // ====================================================

            byte[] liveImageBytes;

            try {

                liveImageBytes =

                        Base64.getDecoder()

                                .decode(base64Image);

            } catch (IllegalArgumentException e) {

                return showError(

                        model,

                        customer,

                        request,

                        "Invalid camera image data."

                );

            }

            if (liveImageBytes.length == 0) {

                return showError(

                        model,

                        customer,

                        request,

                        "Captured face image is empty."

                );

            }

            // ====================================================

            // CREATE LIVE FACE DIRECTORY

            // ====================================================

            Path liveDirectory =

                    Paths.get(

                            LIVE_FACE_DIRECTORY

                    );

            Files.createDirectories(

                    liveDirectory

            );

            // ====================================================

            // SAVE LIVE IMAGE

            // ====================================================

            String safeCustomerId =

                    sanitizeFileName(customerId);

            String liveFileName =

                    safeCustomerId

                            + "_request_"

                            + requestId

                            + "_live_"

                            + System.currentTimeMillis()

                            + ".jpg";

            Path livePath =

                    liveDirectory.resolve(

                            liveFileName

                    );

            Files.write(

                    livePath,

                    liveImageBytes

            );

            // ====================================================

            // EXTRACT FACE FROM PROOF

            // ====================================================

            String proofFacePath =

                    proofPhotoExtractionService.extractFace(

                            proofPath.toString(),

                            customerId,

                            requestId

                    );

            if (proofFacePath == null ||

                    proofFacePath.trim().isEmpty()) {

                markVerificationFailed(

                        request,

                        "FAILED"

                );

                return showError(

                        model,

                        customer,

                        request,

                        "No face could be detected in the uploaded proof."

                );

            }

            // ====================================================

            // SAVE IMAGE PATHS

            // ====================================================

            request.setProofPhoto(

                    proofFacePath

            );

            request.setLiveFacePhoto(

                    livePath.toString()

            );

            customerRequestRepository.save(

                    request

            );

            // ====================================================

            // FACE VERIFICATION

            // ====================================================

            FaceVerificationResult result =

                    faceVerificationService.verifyFaces(

                            proofFacePath,

                            liveImageBytes

                    );

            if (result == null) {

                markVerificationFailed(

                        request,

                        "FAILED"

                );

                return showError(

                        model,

                        customer,

                        request,

                        "Face verification returned no result."

                );

            }

            // ====================================================

            // SAVE MATCH SCORE

            // ====================================================

            double similarity =

                    result.getSimilarity();

            if (Double.isNaN(similarity) ||

                    Double.isInfinite(similarity)) {

                similarity = 0.0;

            }

            request.setFaceMatchScore(

                    similarity

            );

            // ====================================================

            // FACE DID NOT MATCH

            // ====================================================

            if (!result.isMatched()) {

                request.setVerificationStatus(

                        "FAILED"

                );

                request.setStatus(

                        "FAILED"

                );

                request.setCompletedDate(

                        LocalDateTime.now()

                );

                customerRequestRepository.save(

                        request

                );

                model.addAttribute(

                        "customer",

                        customer

                );

                model.addAttribute(

                        "request",

                        request

                );

                model.addAttribute(

                        "matchScore",

                        similarity

                );

                model.addAttribute(

                        "error",

                        "Face verification failed. " +

                                "The proof photo and live face do not match."

                );

                return "face-verification";

            }

            // ====================================================

            // FACE MATCHED

            // ====================================================

            System.out.println(

                    "=============================================="

            );

            System.out.println(

                    "FACE MATCH SUCCESSFUL"

            );

            System.out.println(

                    "Customer ID : "

                            + customerId

            );

            System.out.println(

                    "Request ID  : "

                            + requestId

            );

            System.out.println(

                    "Match Score : "

                            + similarity

            );

            System.out.println(

                    "=============================================="

            );

            // ====================================================

            // UPDATE CUSTOMER

            // ====================================================

            updateCustomerField(

                    customer,

                    request.getFieldName(),

                    request.getNewValue()

            );

            customerRepository.save(

                    customer

            );

            // ====================================================

            // CREATE KYC HISTORY

            // ====================================================

            KycHistory history =

                    new KycHistory();

            history.setCustomerId(

                    customerId

            );

            history.setRequestId(

                    request.getId()

            );

            history.setFieldName(

                    request.getFieldName()

            );

            history.setOldValue(

                    request.getOldValue()

            );

            history.setNewValue(

                    request.getNewValue()

            );

            history.setProofType(

                    request.getVerificationType()

            );

            history.setVerificationStatus(

                    "VERIFIED"

            );

            history.setFaceMatchScore(

                    similarity

            );

            history.setChangedAt(

                    LocalDateTime.now()

            );

            kycHistoryRepository.save(

                    history

            );

            // ====================================================

            // GENERATE UPDATED KYC PDF

            // ====================================================

            String unsignedPdfPath =

                    ekycPdfService.generateUpdatedKycPdf(

                            customer,

                            request

                    );

            Path unsignedPdf =

                    Paths.get(

                            unsignedPdfPath

                    )

                            .toAbsolutePath()

                            .normalize();

            if (!Files.exists(unsignedPdf) ||

                    !Files.isRegularFile(unsignedPdf)) {

                throw new IOException(

                        "Generated KYC PDF was not found."

                );

            }

            byte[] unsignedPdfBytes =

                    Files.readAllBytes(

                            unsignedPdf

                    );

            System.out.println(

                    "Unsigned PDF size: "

                            + unsignedPdfBytes.length

                            + " bytes"

            );

            // ====================================================

            // DIGITALLY SIGN PDF

            // ====================================================

            byte[] signedPdfBytes =

                    pdfSigningService.signPdf(

                            unsignedPdfBytes

                    );

            if (signedPdfBytes == null ||

                    signedPdfBytes.length == 0) {

                throw new IOException(

                        "Digital signing returned an empty PDF."

                );

            }

            // ====================================================

            // SAVE SIGNED PDF

            // ====================================================

            Path customerPdfDirectory =

                    Paths.get(

                            EKYC_DIRECTORY,

                            safeCustomerId

                    )

                            .toAbsolutePath()

                            .normalize();

            Files.createDirectories(

                    customerPdfDirectory

            );

            int versionNumber =

                    getNextVersionNumber(

                            request

                    );

            String signedFileName =

                    "KYC_"

                            + safeCustomerId

                            + "_V"

                            + versionNumber

                            + "_SIGNED.pdf";

            Path signedPdfPath =

                    customerPdfDirectory

                            .resolve(

                                    signedFileName

                            )

                            .normalize();

            if (!signedPdfPath.startsWith(

                    customerPdfDirectory

            )) {

                throw new IOException(

                        "Invalid signed PDF path."

                );

            }

            Files.write(

                    signedPdfPath,

                    signedPdfBytes,

                    StandardOpenOption.CREATE,

                    StandardOpenOption.TRUNCATE_EXISTING,

                    StandardOpenOption.WRITE

            );

            // ====================================================

            // CALCULATE SHA-256 HASH OF SIGNED PDF

            // ====================================================

            String documentHash =

                    calculateSha256(

                            signedPdfBytes

                    );

            // ====================================================

            // GET EKYC RECORD

            // ====================================================

            Ekyc ekyc =

                    getOrCreateCustomerEkyc(

                            customerId

                    );

            if (ekyc == null) {

                throw new IllegalStateException(

                        "eKYC record not found for customer: "

                                + customerId

                );

            }

            // ====================================================

            // SAVE DOCUMENT VERSION

            // ====================================================

            EkycDocumentVersion documentVersion =

                    new EkycDocumentVersion();

            documentVersion.setEkycId(

                    ekyc.getId()

            );

            documentVersion.setVersionNumber(

                    versionNumber

            );

            documentVersion.setFileName(

                    signedFileName

            );

            documentVersion.setFilePath(

                    signedPdfPath.toString()

            );

            documentVersion.setDocumentHash(

                    documentHash

            );

            documentVersion.setSignatureStatus(

                    "SIGNED"

            );

            documentVersion.setSignedAt(

                    LocalDateTime.now()

            );

            documentVersion.setCreatedAt(

                    LocalDateTime.now()

            );

            ekycDocumentVersionRepository.save(

                    documentVersion

            );

            // ====================================================

            // COMPLETE REQUEST ONLY AFTER SIGNED PDF IS SAVED

            // ====================================================

            request.setVerificationStatus(

                    "VERIFIED"

            );

            request.setStatus(

                    "COMPLETED"

            );

            request.setCompletedDate(

                    LocalDateTime.now()

            );

            customerRequestRepository.save(

                    request

            );

            // ====================================================

            // SESSION VALUES

            // ====================================================

            session.setAttribute(

                    "completedKycRequestId",

                    request.getId()

            );

            session.setAttribute(

                    "kycRequestId",

                    request.getId()

            );

            session.setAttribute(

                    "faceMatchScore",

                    similarity

            );

            session.setAttribute(

                    "signedKycPdfPath",

                    signedPdfPath.toString()

            );

            session.setAttribute(

                    "kycDocumentVersionId",

                    documentVersion.getId()

            );

            session.setAttribute(

                    "kycUpdateSuccess",

                    "Face verification successful. "

                            + "Your KYC information has been updated "

                            + "and your digitally signed KYC PDF has been generated."

            );

            // ====================================================

            // FINAL LOG

            // ====================================================

            System.out.println(

                    "=============================================="

            );

            System.out.println(

                    "KYC UPDATE COMPLETED SUCCESSFULLY"

            );

            System.out.println(

                    "Customer ID : "

                            + customerId

            );

            System.out.println(

                    "Request ID  : "

                            + requestId

            );

            System.out.println(

                    "Version     : "

                            + versionNumber

            );

            System.out.println(

                    "Signed PDF  : "

                            + signedPdfPath

            );

            System.out.println(

                    "SHA-256     : "

                            + documentHash

            );

            System.out.println(

                    "=============================================="

            );

            return "redirect:/kyc-update-success";

        } catch (IllegalArgumentException e) {

            e.printStackTrace();

            model.addAttribute(

                    "customer",

                    customer

            );

            model.addAttribute(

                    "request",

                    request

            );

            model.addAttribute(

                    "error",

                    e.getMessage() != null

                            ? e.getMessage()

                            : "Invalid face verification data."

            );

            return "face-verification";

        } catch (IOException e) {

            e.printStackTrace();

            model.addAttribute(

                    "customer",

                    customer

            );

            model.addAttribute(

                    "request",

                    request

            );

            model.addAttribute(

                    "error",

                    "The KYC information was verified, but the PDF could not be generated or signed. Please try again."

            );

            return "face-verification";

        } catch (Exception e) {

            e.printStackTrace();

            model.addAttribute(

                    "customer",

                    customer

            );

            model.addAttribute(

                    "request",

                    request

            );

            model.addAttribute(

                    "error",

                    "KYC processing failed: "

                            + (

                            e.getMessage() != null

                                    ? e.getMessage()

                                    : "Unknown error."

                    )

            );

            return "face-verification";

        }

    }

    // ============================================================

    // DOWNLOAD LATEST SIGNED KYC PDF

    // ============================================================

    @GetMapping("/customer/kyc/download-signed")

    public ResponseEntity<Resource> downloadSignedKycPdf(

            HttpSession session) {

        String customerId =

                (String) session.getAttribute(

                        "loggedInCustomerId"

                );

        if (customerId == null ||

                customerId.trim().isEmpty()) {

            return ResponseEntity

                    .status(401)

                    .build();

        }

        try {

            Ekyc ekyc =

                    getCustomerEkyc(

                            customerId

                    );

            if (ekyc == null) {

                return ResponseEntity

                        .notFound()

                        .build();

            }

            EkycDocumentVersion documentVersion =

                    ekycDocumentVersionRepository

                            .findTopByEkycIdOrderByVersionNumberDesc(

                                    ekyc.getId()

                            )

                            .orElse(null);

            if (documentVersion == null) {

                return ResponseEntity

                        .notFound()

                        .build();

            }

            if (!"SIGNED".equalsIgnoreCase(

                    documentVersion.getSignatureStatus()

            )) {

                return ResponseEntity

                        .notFound()

                        .build();

            }

            String storedPath =

                    documentVersion.getFilePath();

            if (storedPath == null ||

                    storedPath.trim().isEmpty()) {

                return ResponseEntity

                        .notFound()

                        .build();

            }

            Path baseDirectory =

                    Paths.get(

                            EKYC_DIRECTORY

                    )

                            .toAbsolutePath()

                            .normalize();

            Path requestedPath =

                    Paths.get(

                            storedPath

                    )

                            .toAbsolutePath()

                            .normalize();

            if (!requestedPath.startsWith(

                    baseDirectory

            )) {

                return ResponseEntity

                        .status(403)

                        .build();

            }

            if (!Files.exists(requestedPath) ||

                    !Files.isRegularFile(requestedPath)) {

                return ResponseEntity

                        .notFound()

                        .build();

            }

            Resource resource =

                    new FileSystemResource(

                            requestedPath.toFile()

                    );

            String fileName =

                    documentVersion.getFileName();

            return ResponseEntity.ok()

                    .contentType(

                            MediaType.APPLICATION_PDF

                    )

                    .contentLength(

                            Files.size(requestedPath)

                    )

                    .header(

                            HttpHeaders.CONTENT_DISPOSITION,

                            "attachment; filename=\""

                                    + fileName

                                    + "\""

                    )

                    .body(resource);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity

                    .internalServerError()

                    .build();

        }

    }

    // ============================================================

    // UPDATE CUSTOMER FIELD

    // ============================================================

    private void updateCustomerField(

            Customer customer,

            String fieldName,

            String newValue) {

        if (customer == null) {

            throw new IllegalArgumentException(

                    "Customer is null."

            );

        }

        if (fieldName == null ||

                fieldName.trim().isEmpty()) {

            throw new IllegalArgumentException(

                    "KYC field is missing."

            );

        }

        if (newValue == null ||

                newValue.trim().isEmpty()) {

            throw new IllegalArgumentException(

                    "New KYC value is missing."

            );

        }

        switch (fieldName) {

            case "AADHAAR_NAME":

                customer.setFullName(

                        newValue.trim()

                );

                break;

            case "AADHAAR_DOB":

                customer.setDateOfBirth(

                        LocalDate.parse(

                                newValue.trim()

                        )

                );

                break;

            case "AADHAAR_MOBILE":

                customer.setMobile(

                        newValue.trim()

                );

                break;

            case "AADHAAR_ADDRESS":

                customer.setAddress(

                        newValue.trim()

                );

                break;

            case "PAN_NAME":

                customer.setFullName(

                        newValue.trim()

                );

                break;

            case "PAN_DOB":

                customer.setDateOfBirth(

                        LocalDate.parse(

                                newValue.trim()

                        )

                );

                break;

            case "PAN_ADDRESS":

                customer.setAddress(

                        newValue.trim()

                );

                break;

            case "DRIVING_LICENSE_NAME":

                customer.setFullName(

                        newValue.trim()

                );

                break;

            case "DRIVING_LICENSE_DOB":

                customer.setDateOfBirth(

                        LocalDate.parse(

                                newValue.trim()

                        )

                );

                break;

            case "DRIVING_LICENSE_MOBILE":

                customer.setMobile(

                        newValue.trim()

                );

                break;

            case "DRIVING_LICENSE_ADDRESS":

                customer.setAddress(

                        newValue.trim()

                );

                break;

            case "DRIVING_LICENSE_STATE":

                customer.setState(

                        newValue.trim()

                );

                break;

            case "DRIVING_LICENSE_PINCODE":

                customer.setPincode(

                        newValue.trim()

                );

                break;

            default:

                throw new IllegalArgumentException(

                        "Unsupported KYC field: "

                                + fieldName

                );

        }

    }

    // ============================================================

    // CALCULATE SHA-256

    // ============================================================

    private String calculateSha256(

            byte[] data) throws Exception {

        MessageDigest digest =

                MessageDigest.getInstance(

                        "SHA-256"

                );

        byte[] hash =

                digest.digest(data);

        return HexFormat.of().formatHex(hash);

    }

    // ============================================================

    // GET NEXT DOCUMENT VERSION

    // ============================================================

    private int getNextVersionNumber(

            CustomerRequest request) {

        String customerId =

                request.getCustomerId();

        Ekyc ekyc =

                    getCustomerEkyc(

                            customerId

                    );

        if (ekyc == null) {

            return 1;

        }

        EkycDocumentVersion latest =

                ekycDocumentVersionRepository

                        .findTopByEkycIdOrderByVersionNumberDesc(

                                ekyc.getId()

                        )

                        .orElse(null);

        if (latest == null ||

                latest.getVersionNumber() == null) {

            return 1;

        }

        return latest.getVersionNumber() + 1;

    }

    // ============================================================

    // MARK REQUEST FAILED

    // ============================================================

    private void markVerificationFailed(

            CustomerRequest request,

            String status) {

        request.setVerificationStatus(

                status

        );

        request.setStatus(

                status

        );

        request.setCompletedDate(

                LocalDateTime.now()

        );

        customerRequestRepository.save(

                request

        );

    }

    // ============================================================

    // SHOW ERROR

    // ============================================================

    private String showError(

            Model model,

            Customer customer,

            CustomerRequest request,

            String message) {

        model.addAttribute(

                "customer",

                customer

        );

        model.addAttribute(

                "request",

                request

        );

        model.addAttribute(

                "error",

                message

        );

        return "face-verification";

    }

    // ============================================================

    // ============================================================

    // GET CUSTOMER EKYC RECORD

    // ============================================================

    private Ekyc getCustomerEkyc(String customerId) {

        List<Ekyc> ekycList =
                ekycRepository.findByCustomerId(
                        customerId
                );

        if (ekycList == null ||
                ekycList.isEmpty()) {

            return null;
        }

        return ekycList.get(0);
    }

    // ============================================================
    // GET OR CREATE CUSTOMER EKYC RECORD
    // ============================================================

    private Ekyc getOrCreateCustomerEkyc(String customerId) {

        Ekyc existingEkyc =
                getCustomerEkyc(
                        customerId
                );

        if (existingEkyc != null) {
            return existingEkyc;
        }

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElse(null);

        if (customer == null) {
            return null;
        }

        Ekyc ekyc =
                new Ekyc();

        ekyc.setCustomerId(
                customerId
        );

        ekyc.setAadhaarNumber(
                customer.getAadhaarNumber()
        );

        ekyc.setPanNumber(
                customer.getPanNumber()
        );

        ekyc.setDocumentName(
                "KYC Document"
        );

        ekyc.setStatus(
                "VERIFIED"
        );

        ekyc.setSubmittedAt(
                LocalDateTime.now()
        );

        ekyc.setVerifiedAt(
                LocalDateTime.now()
        );

        return ekycRepository.save(
                ekyc
        );
    }

    // SANITIZE FILE NAME

    // ============================================================

    private String sanitizeFileName(

            String value) {

        if (value == null ||

                value.trim().isEmpty()) {

            return "customer";

        }

        return value.replaceAll(

                "[^a-zA-Z0-9_-]",

                "_"

        );

    }

}
