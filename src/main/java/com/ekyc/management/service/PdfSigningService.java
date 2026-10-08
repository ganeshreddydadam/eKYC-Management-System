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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Enumeration;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

@Service
public class PdfSigningService {

    // =================================================
    // Certificate configuration
    // =================================================

    /*
     * This can point to either:
     *
     * certificates/mycertificate.p12
     *
     * OR
     *
     * certificates/mycertificate.pfx
     */
    @Value("${ekyc.signing.certificate}")
    private String certificatePath;

    /*
     * Password of the P12/PFX.
     *
     * Do NOT hard-code the password in this Java class.
     */
    @Value("${ekyc.signing.password}")
    private String certificatePassword;

    /*
     * Reason is signing metadata.
     * It does not come from the certificate.
     */
    @Value("${ekyc.signing.reason:eKYC Document Approval}")
    private String signingReason;

    /*
     * =================================================
     * Main PDF signing method
     * =================================================
     */
    public byte[] signPdf(byte[] unsignedPdf) throws Exception {

        // =================================================
        // 1. Load PKCS12 certificate
        // =================================================

        KeyStore keyStore =
                KeyStore.getInstance("PKCS12");

        try (InputStream inputStream =
                     getClass()
                             .getClassLoader()
                             .getResourceAsStream(certificatePath)) {

            if (inputStream == null) {

                throw new IllegalStateException(
                        "Certificate file not found: "
                                + certificatePath
                );
            }

            keyStore.load(
                    inputStream,
                    certificatePassword.toCharArray()
            );
        }

        // =================================================
        // 2. Automatically find PrivateKeyEntry
        // =================================================

        String keyAlias = findPrivateKeyAlias(keyStore);

        if (keyAlias == null) {

            throw new IllegalStateException(
                    "No PrivateKeyEntry found in certificate: "
                            + certificatePath
            );
        }

        // =================================================
        // 3. Get private key
        // =================================================

        Key key =
                keyStore.getKey(
                        keyAlias,
                        certificatePassword.toCharArray()
                );

        if (!(key instanceof PrivateKey)) {

            throw new IllegalStateException(
                    "Private key not found for alias: "
                            + keyAlias
            );
        }

        PrivateKey privateKey =
                (PrivateKey) key;

        // =================================================
        // 4. Get certificate chain
        // =================================================

        Certificate[] certificateChain =
                keyStore.getCertificateChain(
                        keyAlias
                );

        if (certificateChain == null
                || certificateChain.length == 0) {

            throw new IllegalStateException(
                    "Certificate chain not found for alias: "
                            + keyAlias
            );
        }

        // =================================================
        // 5. Get signing certificate
        // =================================================

        if (!(certificateChain[0]
                instanceof X509Certificate)) {

            throw new IllegalStateException(
                    "The signing certificate is not an X509 certificate."
            );
        }

        X509Certificate signingCertificate =
                (X509Certificate) certificateChain[0];

        // =================================================
        // 6. Automatically read certificate details
        // =================================================

        String signerName =
                getCommonName(
                        signingCertificate.getSubjectX500Principal()
                );

        if (signerName == null
                || signerName.trim().isEmpty()) {

            signerName =
                    signingCertificate
                            .getSubjectX500Principal()
                            .getName();
        }

        String certificateLocation =
                getCertificateLocation(
                        signingCertificate
                );

        String certificateAlgorithm =
                signingCertificate
                        .getPublicKey()
                        .getAlgorithm();

        // =================================================
        // 7. Signing algorithm
        // =================================================

        /*
         * The PDF digest/signing algorithm used by this
         * application is SHA-256.
         *
         * RSA certificate:
         * SHA-256 with RSA
         *
         * EC certificate:
         * SHA-256 with ECDSA
         */
        String signatureAlgorithm =
                "SHA-256";

        // =================================================
        // 8. Prepare input PDF
        // =================================================

        PdfReader reader =
                new PdfReader(
                        new ByteArrayInputStream(
                                unsignedPdf
                        )
                );

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        // =================================================
        // 9. Create PdfSigner
        // =================================================

        PdfSigner signer =
                new PdfSigner(
                        reader,
                        outputStream,
                        new StampingProperties()
                );

        // =================================================
        // 10. Load Windows Unicode font
        // =================================================

        String fontPath =
                "C:\\Windows\\Fonts\\seguisym.ttf";

        PdfFont checkFont =
                PdfFontFactory.createFont(
                        fontPath,
                        "Identity-H",
                        signer.getDocument()
                );

        // =================================================
        // 11. Create signing date
        // =================================================

        String signingDate =
                LocalDateTime
                        .now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd HH:mm:ss"
                                )
                        );

        // =================================================
        // 12. Create signature properties
        // =================================================

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

        // =================================================
        // 13. Create visible signature appearance
        // =================================================

        Div signatureContent =
                new Div();

        signatureContent
                .setPadding(5);

        // =================================================
        // Signature Valid + Green Check Mark
        // =================================================

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

        // =================================================
        // Digitally signed by
        // =================================================

        signatureContent.add(
                new Paragraph(
                        "Digitally signed by: "
                                + signerName
                )
                        .setFontSize(7)
                        .setMargin(0)
        );

        // =================================================
        // Reason
        // =================================================

        signatureContent.add(
                new Paragraph(
                        "Reason: "
                                + signingReason
                )
                        .setFontSize(7)
                        .setMargin(0)
        );

        // =================================================
        // Location
        // =================================================

        signatureContent.add(
                new Paragraph(
                        "Location: "
                                + certificateLocation
                )
                        .setFontSize(7)
                        .setMargin(0)
        );

        // =================================================
        // Algorithm
        // =================================================

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

        // =================================================
        // Date
        // =================================================

        signatureContent.add(
                new Paragraph(
                        "Date: "
                                + signingDate
                )
                        .setFontSize(7)
                        .setMargin(0)
        );

        // =================================================
        // 14. Create signature field appearance
        // =================================================

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

        // =================================================
        // 15. Apply signer properties
        // =================================================

        signer.setSignerProperties(
                signerProperties
        );

        // =================================================
        // 16. Create external signature
        // =================================================

        IExternalSignature signature =
                new PrivateKeySignature(
                        privateKey,
                        DigestAlgorithms.SHA256,
                        "BC"
                );

        // =================================================
        // 17. Create digest
        // =================================================

        IExternalDigest digest =
                new BouncyCastleDigest();

        // =================================================
        // 18. Cryptographically sign PDF
        // =================================================

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

        // =================================================
        // 19. Return signed PDF
        // =================================================

        return outputStream.toByteArray();
    }

    // =================================================
    // Automatically find private key alias
    // =================================================

    private String findPrivateKeyAlias(
            KeyStore keyStore
    ) throws Exception {

        Enumeration<String> aliases =
                keyStore.aliases();

        while (aliases.hasMoreElements()) {

            String alias =
                    aliases.nextElement();

            if (keyStore.isKeyEntry(alias)) {

                Key key =
                        keyStore.getKey(
                                alias,
                                certificatePassword.toCharArray()
                        );

                if (key instanceof PrivateKey) {

                    return alias;
                }
            }
        }

        return null;
    }

    // =================================================
    // Get Common Name from certificate
    // =================================================

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

    // =================================================
    // Get Location from certificate
    // =================================================

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

                // Locality
                if ("L".equalsIgnoreCase(type)) {

                    locality = value;
                }

                // State
                else if ("ST".equalsIgnoreCase(type)) {

                    state = value;
                }

                // Country
                else if ("C".equalsIgnoreCase(type)) {

                    country = value;
                }
            }

        } catch (InvalidNameException e) {

            return "Not specified in certificate";
        }

        // =================================================
        // Build location
        // =================================================

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

    // =================================================
    // Convert country code to country name
    // =================================================

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