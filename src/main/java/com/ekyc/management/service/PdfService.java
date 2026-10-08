package com.ekyc.management.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
public class PdfService {

    private static final String DOCUMENT_FOLDER =
            "generated-documents";


    // =====================================================
    // GENERATE UPDATED KYC PDF
    // =====================================================

    public String generateUpdatedKycPdf(
            Customer customer,
            CustomerRequest request)
            throws IOException {

        // =================================================
        // CREATE PDF FOLDER
        // =================================================

        Files.createDirectories(
                Paths.get(DOCUMENT_FOLDER)
        );


        // =================================================
        // PDF FILE NAME
        // =================================================

        String fileName =
                customer.getCustomerId()
                + "-updated-kyc.pdf";


        Path filePath =
                Paths.get(
                        DOCUMENT_FOLDER,
                        fileName
                );


        // =================================================
        // DETERMINE DOCUMENT TYPE
        // =================================================

        String documentType =
                getDocumentType(request);


        // =================================================
        // CREATE PDF
        // =================================================

        try (PDDocument document =
                     new PDDocument()) {

            PDPage page =
                    new PDPage(PDRectangle.A4);

            document.addPage(page);


            try (PDPageContentStream content =
                         new PDPageContentStream(
                                 document,
                                 page
                         )) {

                // =================================================
                // FONTS
                // =================================================

                PDType1Font boldFont =
                        new PDType1Font(
                                Standard14Fonts.FontName.HELVETICA_BOLD
                        );

                PDType1Font normalFont =
                        new PDType1Font(
                                Standard14Fonts.FontName.HELVETICA
                        );


                // =================================================
                // DOCUMENT BORDER
                // =================================================

                content.setLineWidth(2);

                content.addRect(
                        50,
                        150,
                        495,
                        620
                );

                content.stroke();


                // =================================================
                // DOCUMENT TYPE HEADER
                // =================================================

                content.beginText();

                content.setFont(
                        boldFont,
                        24
                );

                content.newLineAtOffset(
                        getHeaderX(documentType),
                        730
                );

                content.showText(
                        documentType
                );

                content.endText();


                // =================================================
                // SUB HEADER
                // =================================================

                content.beginText();

                content.setFont(
                        normalFont,
                        10
                );

                content.newLineAtOffset(
                        80,
                        700
                );

                content.showText(
                        "UPDATED KYC INFORMATION"
                );

                content.endText();


                // =================================================
                // DOCUMENT CONTENT
                // =================================================

                if ("AADHAAR".equals(documentType)) {

                    generateAadhaarContent(
                            content,
                            customer,
                            boldFont,
                            normalFont
                    );

                } else if ("PAN".equals(documentType)) {

                    generatePanContent(
                            content,
                            customer,
                            boldFont,
                            normalFont
                    );

                } else if (
                        "DRIVING LICENCE".equals(documentType)
                ) {

                    generateDrivingLicenceContent(
                            content,
                            customer,
                            boldFont,
                            normalFont
                    );

                } else {

                    generateGenericKycContent(
                            content,
                            customer,
                            boldFont,
                            normalFont
                    );
                }


                // =================================================
                // SIGNATURE
                // =================================================

                


                // =================================================
                // APPROVED STATUS
                // =================================================

                content.beginText();

                content.setFont(
                        boldFont,
                        11
                );

                content.newLineAtOffset(
                        80,
                        175
                );

                content.showText(
                        "STATUS : APPROVED"
                );

                content.endText();


                // =================================================
                // FOOTER
                // =================================================

                content.beginText();

                content.setFont(
                        normalFont,
                        8
                );

                content.newLineAtOffset(
                        120,
                        125
                );

                content.showText(
                        "Updated KYC record generated by eKYC Management System"
                );

                content.endText();
            }


            // =================================================
            // SAVE PDF
            // =================================================

            document.save(
                    filePath.toString()
            );
        }


        return filePath.toString();
    }


    // =====================================================
    // AADHAAR CONTENT
    // =====================================================

    private void generateAadhaarContent(
            PDPageContentStream content,
            Customer customer,
            PDType1Font boldFont,
            PDType1Font normalFont)
            throws IOException {

        // -------------------------------------------------
        // NAME
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Name",
                safe(customer.getFullName()),
                80,
                640
        );


        // -------------------------------------------------
        // DATE OF BIRTH
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Date of Birth",
                safeDate(customer.getDateOfBirth()),
                80,
                595
        );


        // -------------------------------------------------
        // GENDER
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Gender",
                safeGender(customer.getGender()),
                80,
                550
        );


        // -------------------------------------------------
        // AADHAAR NUMBER
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Aadhaar Number",
                maskAadhaar(
                        customer.getAadhaarNumber()
                ),
                80,
                505
        );


        // -------------------------------------------------
        // ADDRESS
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Address",
                safe(customer.getAddress()),
                80,
                460
        );


        // -------------------------------------------------
        // CITY
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "City",
                safe(customer.getCity()),
                80,
                415
        );


        // -------------------------------------------------
        // STATE
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "State",
                safe(customer.getState()),
                80,
                370
        );


        // -------------------------------------------------
        // PINCODE
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Pincode",
                safe(customer.getPincode()),
                80,
                325
        );
    }


    // =====================================================
    // PAN CONTENT
    // =====================================================

    private void generatePanContent(
            PDPageContentStream content,
            Customer customer,
            PDType1Font boldFont,
            PDType1Font normalFont)
            throws IOException {

        // -------------------------------------------------
        // NAME
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Name",
                safe(customer.getFullName()),
                80,
                640
        );


        // -------------------------------------------------
        // FATHER'S NAME
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Father's Name",
                safe(customer.getFatherName()),
                80,
                585
        );


        // -------------------------------------------------
        // DATE OF BIRTH
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Date of Birth",
                safeDate(customer.getDateOfBirth()),
                80,
                530
        );


        // -------------------------------------------------
        // PAN NUMBER
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "PAN Number",
                safe(customer.getPanNumber()),
                80,
                475
        );
    }


    // =====================================================
    // DRIVING LICENCE CONTENT
    // =====================================================

    private void generateDrivingLicenceContent(
            PDPageContentStream content,
            Customer customer,
            PDType1Font boldFont,
            PDType1Font normalFont)
            throws IOException {

        // -------------------------------------------------
        // NAME
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Name",
                safe(customer.getFullName()),
                80,
                640
        );


        // -------------------------------------------------
        // DATE OF BIRTH
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Date of Birth",
                safeDate(customer.getDateOfBirth()),
                80,
                585
        );


        // -------------------------------------------------
        // GENDER
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Gender",
                safeGender(customer.getGender()),
                80,
                530
        );


        // -------------------------------------------------
        // MOBILE
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Mobile",
                safe(customer.getMobile()),
                80,
                475
        );


        // -------------------------------------------------
        // ADDRESS
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Address",
                safe(customer.getAddress()),
                80,
                420
        );


        // -------------------------------------------------
        // CITY
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "City",
                safe(customer.getCity()),
                80,
                365
        );


        // -------------------------------------------------
        // STATE
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "State",
                safe(customer.getState()),
                80,
                310
        );


        // -------------------------------------------------
        // PINCODE
        // -------------------------------------------------

        writeField(
                content,
                boldFont,
                normalFont,
                "Pincode",
                safe(customer.getPincode()),
                80,
                255
        );
    }


    // =====================================================
    // GENERIC KYC CONTENT
    // =====================================================

    private void generateGenericKycContent(
            PDPageContentStream content,
            Customer customer,
            PDType1Font boldFont,
            PDType1Font normalFont)
            throws IOException {

        writeField(
                content,
                boldFont,
                normalFont,
                "Name",
                safe(customer.getFullName()),
                80,
                640
        );


        writeField(
                content,
                boldFont,
                normalFont,
                "Date of Birth",
                safeDate(customer.getDateOfBirth()),
                80,
                585
        );


        writeField(
                content,
                boldFont,
                normalFont,
                "Gender",
                safeGender(customer.getGender()),
                80,
                530
        );


        writeField(
                content,
                boldFont,
                normalFont,
                "Address",
                safe(customer.getAddress()),
                80,
                475
        );


        writeField(
                content,
                boldFont,
                normalFont,
                "City",
                safe(customer.getCity()),
                80,
                420
        );


        writeField(
                content,
                boldFont,
                normalFont,
                "State",
                safe(customer.getState()),
                80,
                365
        );


        writeField(
                content,
                boldFont,
                normalFont,
                "Pincode",
                safe(customer.getPincode()),
                80,
                310
        );
    }


    // =====================================================
    // WRITE FIELD
    // =====================================================

    private void writeField(
            PDPageContentStream content,
            PDType1Font boldFont,
            PDType1Font normalFont,
            String label,
            String value,
            float x,
            float y)
            throws IOException {

        // Label

        content.beginText();

        content.setFont(
                boldFont,
                12
        );

        content.newLineAtOffset(
                x,
                y
        );

        content.showText(
                label + " :"
        );

        content.endText();


        // Value

        content.beginText();

        content.setFont(
                normalFont,
                12
        );

        content.newLineAtOffset(
                x + 130,
                y
        );

        content.showText(
                value
        );

        content.endText();
    }


   
    // =====================================================
    // DETERMINE DOCUMENT TYPE
    // =====================================================

    private String getDocumentType(
            CustomerRequest request) {

        if (request == null) {
            return "UPDATED KYC";
        }


        String requestType =
                request.getRequestType();

        String fieldName =
                request.getFieldName();


        String type =
                requestType == null
                        ? ""
                        : requestType
                                .trim()
                                .toUpperCase();


        String field =
                fieldName == null
                        ? ""
                        : fieldName
                                .trim()
                                .toUpperCase();


        // =================================================
        // AADHAAR
        // =================================================

        if (type.contains("AADHAAR")
                || field.contains("AADHAAR")) {

            return "AADHAAR";
        }


        // =================================================
        // PAN
        // =================================================

        if (type.contains("PAN")
                || field.contains("PAN")) {

            return "PAN";
        }


        // =================================================
        // DRIVING LICENCE
        // =================================================

        if (type.contains("DRIVING")
                || type.contains("LICENCE")
                || type.contains("LICENSE")
                || field.contains("DRIVING")
                || field.contains("LICENCE")
                || field.contains("LICENSE")) {

            return "DRIVING LICENCE";
        }


        return "UPDATED KYC";
    }


    // =====================================================
    // MASK AADHAAR
    // =====================================================

    private String maskAadhaar(
            String aadhaar) {

        if (aadhaar == null ||
                aadhaar.length() < 4) {

            return "XXXX XXXX XXXX";
        }


        return "XXXX XXXX "
                + aadhaar.substring(
                        aadhaar.length() - 4
                );
    }


    // =====================================================
    // SAFE STRING
    // =====================================================

    private String safe(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "Not Available";
        }


        return value;
    }


    // =====================================================
    // SAFE DATE
    // =====================================================

    private String safeDate(
            java.time.LocalDate date) {

        if (date == null) {
            return "Not Available";
        }


        return date.toString();
    }


    // =====================================================
    // SAFE GENDER
    // =====================================================

    private String safeGender(
            Customer.Gender gender) {

        if (gender == null) {
            return "Not Available";
        }


        return gender.toString();
    }


    // =====================================================
    // HEADER POSITION
    // =====================================================

    private float getHeaderX(
            String documentType) {

        if ("DRIVING LICENCE".equals(documentType)) {
            return 175;
        }

        if ("AADHAAR".equals(documentType)) {
            return 235;
        }

        if ("PAN".equals(documentType)) {
            return 275;
        }

        return 220;
    }
}