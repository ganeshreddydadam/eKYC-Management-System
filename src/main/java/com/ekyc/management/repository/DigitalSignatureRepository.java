package com.ekyc.management.repository;

import com.ekyc.management.entity.DigitalSignature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DigitalSignatureRepository
        extends JpaRepository<DigitalSignature, Long> {

    Optional<DigitalSignature>
    findByVerificationToken(String verificationToken);
}