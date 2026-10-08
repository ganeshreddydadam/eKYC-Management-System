package com.ekyc.management.service;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SoftHSMKeyService {

    @Value("${softhsm.config}")
    private String configFile;

    @Value("${softhsm.alias}")
    private String alias;

    @Value("${softhsm.pin}")
    private String pin;

    private Provider pkcs11Provider;

    private KeyStore keyStore;

    /**
     * Initialize connection to SoftHSM.
     */
    private synchronized void initialize() throws Exception {

        if (keyStore != null) {
            return;
        }

        // ---------------------------------------------------------
        // Get Java's SunPKCS11 provider
        // ---------------------------------------------------------
        Provider sunPKCS11 =
                Security.getProvider("SunPKCS11");

        if (sunPKCS11 == null) {
            throw new IllegalStateException(
                    "SunPKCS11 provider is not available."
            );
        }

        // ---------------------------------------------------------
        // Configure SunPKCS11 using the SoftHSM configuration file
        // ---------------------------------------------------------
        pkcs11Provider =
                sunPKCS11.configure(configFile);

        // ---------------------------------------------------------
        // Add configured provider if not already registered
        // ---------------------------------------------------------
        if (Security.getProvider(pkcs11Provider.getName()) == null) {
            Security.addProvider(pkcs11Provider);
        }

        // ---------------------------------------------------------
        // Open PKCS#11 keystore
        // ---------------------------------------------------------
        keyStore =
                KeyStore.getInstance(
                        "PKCS11",
                        pkcs11Provider
                );

        // ---------------------------------------------------------
        // Login to SoftHSM
        // ---------------------------------------------------------
        keyStore.load(
                null,
                pin.toCharArray()
        );

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "Connected to SoftHSM successfully."
        );

        System.out.println(
                "Provider: "
                        + pkcs11Provider.getName()
        );

        System.out.println(
                "Token alias: "
                        + alias
        );

        // ---------------------------------------------------------
        // Verify that the configured alias exists
        // ---------------------------------------------------------
        if (!keyStore.containsAlias(alias)) {

            throw new IllegalStateException(
                    "Alias '" + alias
                            + "' was not found in SoftHSM."
            );
        }

        System.out.println(
                "SoftHSM alias found: "
                        + alias
        );

        System.out.println(
                "=============================================="
        );
    }

    /**
     * Returns the private key stored inside SoftHSM.
     *
     * The private key remains PKCS#11-backed.
     * It is not extracted as raw key material.
     */
    public PrivateKey getPrivateKey()
            throws Exception {

        initialize();

        PrivateKey privateKey =
                (PrivateKey) keyStore.getKey(
                        alias,
                        null
                );

        if (privateKey == null) {

            throw new IllegalStateException(
                    "Private key not found for alias: "
                            + alias
            );
        }

        return privateKey;
    }

    /**
     * Returns the X.509 certificate stored in SoftHSM.
     */
    public Certificate getCertificate()
            throws Exception {

        initialize();

        Certificate certificate =
                keyStore.getCertificate(alias);

        if (certificate == null) {

            throw new IllegalStateException(
                    "Certificate not found for alias: "
                            + alias
            );
        }

        return certificate;
    }

    /**
     * Returns the SoftHSM keystore.
     */
    public KeyStore getKeyStore()
            throws Exception {

        initialize();

        return keyStore;
    }

    /**
     * Returns the SoftHSM alias.
     */
    public String getAlias() {

        return alias;
    }

    /**
     * Returns the name of the PKCS#11 provider.
     *
     * This is important for iText PDF signing.
     *
     * The returned value should normally be:
     *
     * SunPKCS11-SoftHSM
     *
     * iText will use this provider for the actual
     * ECDSA private-key signing operation.
     */
    public String getProviderName()
            throws Exception {

        initialize();

        return pkcs11Provider.getName();
    }

    /**
     * Returns the configured PKCS#11 provider.
     */
    public Provider getProvider()
            throws Exception {

        initialize();

        return pkcs11Provider;
    }
}