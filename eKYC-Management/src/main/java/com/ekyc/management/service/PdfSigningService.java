package com.ekyc.management.service;

import com.itextpdf.forms.form.element.SignatureFieldAppearance;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.crypto.DigestAlgorithms;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import com.itextpdf.signatures.SignerProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

@Service
public class PdfSigningService {

    // =========================================================
    // SoftHSM service
    // =========================================================

    @Autowired
    private SoftHSMKeyService softHSMKeyService;

    // =========================================================
    // Signing reason
    // =========================================================

    /*
     * This is signing metadata.
     *
     * It is NOT the certificate password.
     *
     * It is only displayed in the PDF signature information.
     */
    @Value("${ekyc.signing.reason:eKYC Document Approval}")
    private String signingReason;


    // =========================================================
    // Main PDF signing method
    // =========================================================

    public byte[] signPdf(byte[] unsignedPdf) throws Exception {

        // =====================================================
        // 1. Get private key from SoftHSM
        // =====================================================

        /*
         * IMPORTANT:
         *
         * This private key is backed by SoftHSM through
         * SunPKCS11.
         *
         * The private key is NOT loaded from a P12/PFX file.
         */
        PrivateKey privateKey =
                softHSMKeyService.getPrivateKey();

        if (privateKey == null) {

            throw new IllegalStateException(
                    "Private key could not be loaded from SoftHSM."
            );
        }


        // =====================================================
        // 2. Get certificate from SoftHSM
        // =====================================================

        Certificate signingCertificate =
                softHSMKeyService.getCertificate();

        if (signingCertificate == null) {

            throw new IllegalStateException(
                    "Certificate could not be loaded from SoftHSM."
            );
        }


        // =====================================================
        // 3. Create certificate chain
        // =====================================================

        /*
         * Your current SoftHSM token contains the signing
         * certificate associated with the EC private key.
         *
         * Therefore we use that certificate as the certificate
         * chain.
         */
        Certificate[] certificateChain =
                new Certificate[] {
                        signingCertificate
                };


        // =====================================================
        // 4. Verify X.509 certificate
        // =====================================================

        if (!(signingCertificate instanceof X509Certificate)) {

            throw new IllegalStateException(
                    "The SoftHSM certificate is not an X.509 certificate."
            );
        }

        X509Certificate x509Certificate =
                (X509Certificate) signingCertificate;


        // =====================================================
        // 5. Read certificate information
        // =====================================================

        String signerName =
                getCommonName(
                        x509Certificate
                                .getSubjectX500Principal()
                );

        if (signerName == null
                || signerName.trim().isEmpty()) {

            signerName =
                    x509Certificate
                            .getSubjectX500Principal()
                            .getName();
        }


        // =====================================================
        // 6. Certificate location
        // =====================================================

        String certificateLocation =
                getCertificateLocation(
                        x509Certificate
                );


        // =====================================================
        // 7. Certificate public-key algorithm
        // =====================================================

        String certificateAlgorithm =
                x509Certificate
                        .getPublicKey()
                        .getAlgorithm();


        // =====================================================
        // 8. Signature algorithm displayed in PDF
        // =====================================================

        String signatureAlgorithm =
                "SHA-256";


        // =====================================================
        // 9. Get SoftHSM provider name
        // =====================================================

        /*
         * This is VERY IMPORTANT.
         *
         * Do NOT use:
         *
         *     "BC"
         *
         * here.
         *
         * The EC private key is inside SoftHSM and is backed by
         * SunPKCS11.
         *
         * Therefore the actual ECDSA operation must be sent to:
         *
         *     SunPKCS11-SoftHSM
         */
        String softHSMProvider =
                softHSMKeyService.getProviderName();


        System.out.println(
                "=============================================="
        );

        System.out.println(
                "SoftHSM PDF signing initialized"
        );

        System.out.println(
                "Token alias: "
                        + softHSMKeyService.getAlias()
        );

        System.out.println(
                "Provider: "
                        + softHSMProvider
        );

        System.out.println(
                "Private Key Algorithm: "
                        + privateKey.getAlgorithm()
        );

        System.out.println(
                "Private Key Format: "
                        + privateKey.getFormat()
        );

        System.out.println(
                "Certificate Type: "
                        + signingCertificate.getType()
        );

        System.out.println(
                "Public Key Algorithm: "
                        + x509Certificate
                                .getPublicKey()
                                .getAlgorithm()
        );

        System.out.println(
                "=============================================="
        );


        // =====================================================
        // 10. Prepare input PDF
        // =====================================================

        PdfReader reader =
                new PdfReader(
                        new ByteArrayInputStream(
                                unsignedPdf
                        )
                );


        // =====================================================
        // 11. Output PDF
        // =====================================================

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();


        // =====================================================
        // 12. Create PdfSigner
        // =====================================================

        PdfSigner signer =
                new PdfSigner(
                        reader,
                        outputStream,
                        new StampingProperties()
                );


        // =====================================================
        // 13. Load Windows Unicode font
        // =====================================================

        String fontPath =
                "C:\\Windows\\Fonts\\seguisym.ttf";

        PdfFont checkFont =
                PdfFontFactory.createFont(
                        fontPath,
                        "Identity-H",
                        signer.getDocument()
                );


        // =====================================================
        // 14. Create signing date
        // =====================================================

        String signingDate =
                LocalDateTime
                        .now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd HH:mm:ss"
                                )
                        );


        // =====================================================
        // 15. Create signature properties
        // =====================================================

        SignerProperties signerProperties =
                new SignerProperties()
                        .setFieldName(
                                "eKYC_Digital_Signature"
                        )
                        .setReason(
                                signingReason
                        )
                        .setLocation(
                                certificateLocation
                        )
                        .setPageRect(
                                new Rectangle(
                                        350,
                                        190,
                                        190,
                                        105
                                )
                        )
                        .setPageNumber(1);


        // =====================================================
        // 16. Create visible signature appearance
        // =====================================================

        Div signatureContent =
                new Div();

        signatureContent
                .setPadding(5);


        // =====================================================
        // Signature Valid + Green Check Mark
        // =====================================================

        Text checkMark =
                new Text("\u2713 ")
                        .setFont(checkFont)
                        .setFontSize(16)
                        .setFontColor(
                                ColorConstants.GREEN
                        );

        Text validText =
                new Text("Signature valid")
                        .setFontSize(11);

        Paragraph validParagraph =
                new Paragraph()
                        .add(checkMark)
                        .add(validText)
                        .setMargin(0);

        signatureContent.add(
                validParagraph
        );


        // =====================================================
        // Digitally signed by
        // =====================================================

        signatureContent.add(
                new Paragraph(
                        "Digitally signed by: "
                                + signerName
                )
                        .setFontSize(7)
                        .setMargin(0)
        );


        // =====================================================
        // Reason
        // =====================================================

        signatureContent.add(
                new Paragraph(
                        "Reason: "
                                + signingReason
                )
                        .setFontSize(7)
                        .setMargin(0)
        );


        // =====================================================
        // Location
        // =====================================================

        signatureContent.add(
                new Paragraph(
                        "Location: "
                                + certificateLocation
                )
                        .setFontSize(7)
                        .setMargin(0)
        );


        // =====================================================
        // Algorithm
        // =====================================================

        signatureContent.add(
                new Paragraph(
                        "Algorithm: "
                                + signatureAlgorithm
                                + " / "
                                + certificateAlgorithm
                )
                        .setFontSize(7)
                        .setMargin(0)
        );


        // =====================================================
        // Date
        // =====================================================

        signatureContent.add(
                new Paragraph(
                        "Date: "
                                + signingDate
                )
                        .setFontSize(7)
                        .setMargin(0)
        );


        // =====================================================
        // 17. Create signature field appearance
        // =====================================================

        SignatureFieldAppearance appearance =
                new SignatureFieldAppearance(
                        SignerProperties.IGNORED_ID
                )
                        .setContent(
                                signatureContent
                        );

        signerProperties.setSignatureAppearance(
                appearance
        );


        // =====================================================
        // 18. Apply signer properties
        // =====================================================

        signer.setSignerProperties(
                signerProperties
        );


        // =====================================================
        // 19. Create external signature
        // =====================================================

        /*
         * =====================================================
         * IMPORTANT CHANGE
         * =====================================================
         *
         * OLD:
         *
         *     new PrivateKeySignature(
         *         privateKey,
         *         DigestAlgorithms.SHA256,
         *         "BC"
         *     );
         *
         * This caused:
         *
         *     Signing algorithms SHA256withECDSA and ECDSA
         *     are not supported.
         *
         * and:
         *
         *     InvalidKeyException:
         *     no encoding for EC private key
         *
         * because Bouncy Castle was trying to interpret the
         * PKCS#11-backed EC private key.
         *
         * NEW:
         *
         *     softHSMProvider
         *
         * This tells iText to use the SunPKCS11 provider that
         * is connected to SoftHSM.
         */

        IExternalSignature signature =
                new PrivateKeySignature(
                        privateKey,
                        DigestAlgorithms.SHA256,
                        softHSMProvider
                );


        // =====================================================
        // 20. Create digest
        // =====================================================

        /*
         * Bouncy Castle is still used here for the digest.
         *
         * This does NOT mean that Bouncy Castle receives or
         * extracts the SoftHSM private key.
         */
        IExternalDigest digest =
                new BouncyCastleDigest();


        // =====================================================
        // 21. Cryptographically sign PDF
        // =====================================================

        System.out.println(
                "Starting ECDSA PDF signing..."
        );

        signer.signDetached(
                digest,
                signature,
                certificateChain,
                null,
                null,
                null,
                0,
                PdfSigner.CryptoStandard.CMS
        );


        // =====================================================
        // 22. Signing successful
        // =====================================================

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "PDF SIGNING SUCCESSFUL"
        );

        System.out.println(
                "Provider: "
                        + softHSMProvider
        );

        System.out.println(
                "Algorithm: SHA256withECDSA"
        );

        System.out.println(
                "Token Alias: "
                        + softHSMKeyService.getAlias()
        );

        System.out.println(
                "Signed PDF Size: "
                        + outputStream.size()
                        + " bytes"
        );

        System.out.println(
                "=============================================="
        );


        // =====================================================
        // 23. Return signed PDF
        // =====================================================

        return outputStream.toByteArray();
    }


    // =========================================================
    // Get Common Name from certificate
    // =========================================================

    private String getCommonName(
            javax.security.auth.x500.X500Principal principal
    ) {

        try {

            LdapName ldapName =
                    new LdapName(
                            principal.getName()
                    );

            for (Rdn rdn :
                    ldapName.getRdns()) {

                if ("CN".equalsIgnoreCase(
                        rdn.getType()
                )) {

                    return String.valueOf(
                            rdn.getValue()
                    );
                }
            }

        } catch (InvalidNameException e) {

            return null;
        }

        return null;
    }


    // =========================================================
    // Get Location from certificate
    // =========================================================

    private String getCertificateLocation(
            X509Certificate certificate
    ) {

        String locality = null;
        String state = null;
        String country = null;

        try {

            LdapName ldapName =
                    new LdapName(
                            certificate
                                    .getSubjectX500Principal()
                                    .getName()
                    );

            for (Rdn rdn :
                    ldapName.getRdns()) {

                String type =
                        rdn.getType();

                String value =
                        String.valueOf(
                                rdn.getValue()
                        );

                // -------------------------------------------------
                // Locality
                // -------------------------------------------------

                if ("L".equalsIgnoreCase(type)) {

                    locality = value;
                }

                // -------------------------------------------------
                // State
                // -------------------------------------------------

                else if ("ST".equalsIgnoreCase(type)) {

                    state = value;
                }

                // -------------------------------------------------
                // Country
                // -------------------------------------------------

                else if ("C".equalsIgnoreCase(type)) {

                    country = value;
                }
            }

        } catch (InvalidNameException e) {

            return "Not specified in certificate";
        }


        // =========================================================
        // Build location
        // =========================================================

        StringBuilder location =
                new StringBuilder();


        if (locality != null
                && !locality.isBlank()) {

            location.append(locality);
        }


        if (state != null
                && !state.isBlank()) {

            if (location.length() > 0) {

                location.append(", ");
            }

            location.append(state);
        }


        if (country != null
                && !country.isBlank()) {

            if (location.length() > 0) {

                location.append(", ");
            }

            location.append(
                    getCountryName(country)
            );
        }


        if (location.length() == 0) {

            return "Not specified in certificate";
        }


        return location.toString();
    }


    // =========================================================
    // Convert country code to country name
    // =========================================================

    private String getCountryName(
            String countryCode
    ) {

        if ("IN".equalsIgnoreCase(
                countryCode
        )) {

            return "India";
        }

        return countryCode;
    }
}