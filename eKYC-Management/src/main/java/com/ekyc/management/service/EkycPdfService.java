package com.ekyc.management.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import com.ekyc.management.model.Customer;
import com.ekyc.management.model.CustomerRequest;

@Service
public class EkycPdfService {

    private static final String PDF_DIRECTORY = "uploads/ekyc";

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final PDType1Font TITLE_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final PDType1Font HEADER_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final PDType1Font NORMAL_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    // ============================================================
    // GENERATE UPDATED KYC PDF
    // ============================================================

    public String generateUpdatedKycPdf(
            Customer customer,
            CustomerRequest request) throws IOException {

        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer cannot be null."
            );
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Customer request cannot be null."
            );
        }

        // ========================================================
        // CUSTOMER DIRECTORY
        // ========================================================

        String customerId =
                safeFileName(customer.getCustomerId());

        Path customerDirectory =
                Paths.get(
                        PDF_DIRECTORY,
                        customerId
                )
                        .toAbsolutePath()
                        .normalize();

        Files.createDirectories(
                customerDirectory
        );

        // ========================================================
        // FILE NAME
        // ========================================================

        String timestamp =
                LocalDateTime.now()
                        .format(FILE_DATE_FORMAT);

        String fileName =
                "KYC_UPDATED_"
                        + customerId
                        + "_REQUEST_"
                        + request.getId()
                        + "_"
                        + timestamp
                        + ".pdf";

        Path pdfPath =
                customerDirectory
                        .resolve(fileName)
                        .normalize();

        if (!pdfPath.startsWith(
                customerDirectory
        )) {

            throw new IOException(
                    "Invalid PDF output path."
            );
        }

        // ========================================================
        // CREATE PDF
        // ========================================================

        try (PDDocument document =
                     new PDDocument()) {

            PDPage page =
                    new PDPage(
                            PDRectangle.A4
                    );

            document.addPage(page);

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(
                                 document,
                                 page
                         )) {

                float pageWidth =
                        PDRectangle.A4.getWidth();

                float pageHeight =
                        PDRectangle.A4.getHeight();

                float margin = 45;

                float y = 800;

                // ====================================================
                // TITLE
                // ====================================================

                String title =
                        "eKYC DOCUMENT";

                contentStream.beginText();

                contentStream.setFont(
                        TITLE_FONT,
                        20
                );

                float titleWidth =
                        TITLE_FONT.getStringWidth(title)
                                / 1000
                                * 20;

                contentStream.newLineAtOffset(
                        (pageWidth - titleWidth) / 2,
                        y
                );

                contentStream.showText(
                        title
                );

                contentStream.endText();

                y -= 30;

                // ====================================================
                // HORIZONTAL LINE
                // ====================================================

                contentStream.moveTo(
                        margin,
                        y
                );

                contentStream.lineTo(
                        pageWidth - margin,
                        y
                );

                contentStream.stroke();

                y -= 35;

                // ====================================================
                // PHOTO
                // ====================================================

                Path proofPhotoPath =
                        getProofPhotoPath(request);

                if (proofPhotoPath != null &&
                        Files.exists(proofPhotoPath) &&
                        Files.isRegularFile(proofPhotoPath)) {

                    try {

                        PDImageXObject photo =
                                PDImageXObject.createFromFile(
                                        proofPhotoPath.toString(),
                                        document
                                );

                        float photoX =
                                pageWidth - margin - 120;

                        float photoY =
                                y - 135;

                        float photoWidth = 100;

                        float photoHeight = 125;

                        contentStream.drawImage(
                                photo,
                                photoX,
                                photoY,
                                photoWidth,
                                photoHeight
                        );

                        // Photo border
                        contentStream.addRect(
                                photoX,
                                photoY,
                                photoWidth,
                                photoHeight
                        );

                        contentStream.stroke();

                    } catch (Exception e) {

                        System.out.println(
                                "Could not add customer photo: "
                                        + e.getMessage()
                        );
                    }
                }

                // ====================================================
                // PERSONAL INFORMATION
                // ====================================================

                y = writeSectionTitle(
                        contentStream,
                        "PERSONAL INFORMATION",
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "Customer ID",
                        customer.getCustomerId(),
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "Full Name",
                        customer.getFullName(),
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "Father Name",
                        customer.getFatherName(),
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "Date of Birth",
                        customer.getDateOfBirth() != null
                                ? customer.getDateOfBirth().toString()
                                : null,
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "Gender",
                        customer.getGender() != null
                                ? customer.getGender().toString()
                                : null,
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "Mobile",
                        customer.getMobile(),
                        margin,
                        y
                );

                y -= 12;

                // ====================================================
                // ADDRESS INFORMATION
                // ====================================================

                y = writeSectionTitle(
                        contentStream,
                        "ADDRESS",
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "Address",
                        customer.getAddress(),
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "City",
                        customer.getCity(),
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "State",
                        customer.getState(),
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "Pincode",
                        customer.getPincode(),
                        margin,
                        y
                );

                y -= 12;

                // ====================================================
                // IDENTITY INFORMATION
                // ====================================================

                y = writeSectionTitle(
                        contentStream,
                        "IDENTITY INFORMATION",
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "Aadhaar Number",
                        customer.getAadhaarNumber(),
                        margin,
                        y
                );

                y = writeField(
                        contentStream,
                        "PAN Number",
                        customer.getPanNumber(),
                        margin,
                        y
                );

                // ====================================================
                // FOOTER
                // ====================================================

                contentStream.beginText();

                contentStream.setFont(
                        NORMAL_FONT,
                        8
                );

                contentStream.newLineAtOffset(
                        margin,
                        30
                );

                contentStream.showText(
                        "Generated by eKYC Management System"
                );

                contentStream.endText();
            }

            // ========================================================
            // SAVE PDF
            // ========================================================

            document.save(
                    pdfPath.toFile()
            );
        }

        // ========================================================
        // CONSOLE LOG
        // ========================================================

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "UPDATED KYC PDF GENERATED"
        );

        System.out.println(
                "Customer ID : "
                        + customer.getCustomerId()
        );

        System.out.println(
                "Request ID  : "
                        + request.getId()
        );

        System.out.println(
                "PDF Path    : "
                        + pdfPath
        );

        System.out.println(
                "=============================================="
        );

        return pdfPath.toString();
    }

    // ============================================================
    // GET PROOF PHOTO PATH
    // ============================================================

    private Path getProofPhotoPath(
            CustomerRequest request) {

        if (request == null) {
            return null;
        }

        String proofPhoto =
                request.getProofPhoto();

        if (proofPhoto == null ||
                proofPhoto.isBlank()) {

            return null;
        }

        try {

            Path path =
                    Paths.get(
                            proofPhoto
                    )
                            .toAbsolutePath()
                            .normalize();

            return path;

        } catch (Exception e) {

            return null;
        }
    }

    // ============================================================
    // SECTION TITLE
    // ============================================================

    private float writeSectionTitle(
            PDPageContentStream contentStream,
            String title,
            float x,
            float y) throws IOException {

        contentStream.beginText();

        contentStream.setFont(
                HEADER_FONT,
                12
        );

        contentStream.newLineAtOffset(
                x,
                y
        );

        contentStream.showText(
                sanitizePdfText(title)
        );

        contentStream.endText();

        return y - 22;
    }

    // ============================================================
    // FIELD
    // ============================================================

    private float writeField(
            PDPageContentStream contentStream,
            String label,
            String value,
            float x,
            float y) throws IOException {

        if (value == null ||
                value.isBlank()) {

            value = "-";
        }

        String text =
                label
                        + " : "
                        + sanitizePdfText(value);

        contentStream.beginText();

        contentStream.setFont(
                NORMAL_FONT,
                10
        );

        contentStream.newLineAtOffset(
                x,
                y
        );

        contentStream.showText(
                text
        );

        contentStream.endText();

        return y - 18;
    }

    // ============================================================
    // SANITIZE PDF TEXT
    // ============================================================

    private String sanitizePdfText(
            String value) {

        if (value == null) {
            return "-";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ");
    }

    // ============================================================
    // SAFE FILE NAME
    // ============================================================

    private String safeFileName(
            String value) {

        if (value == null ||
                value.isBlank()) {

            return "UNKNOWN_CUSTOMER";
        }

        return value.replaceAll(
                "[^a-zA-Z0-9._-]",
                "_"
        );
    }
}