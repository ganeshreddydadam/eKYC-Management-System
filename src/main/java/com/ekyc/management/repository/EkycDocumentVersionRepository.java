package com.ekyc.management.repository;

import com.ekyc.management.entity.EkycDocumentVersion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EkycDocumentVersionRepository
        extends JpaRepository<EkycDocumentVersion, Long> {

    Optional<EkycDocumentVersion>
    findTopByEkycIdOrderByVersionNumberDesc(
            Long ekycId
    );
}