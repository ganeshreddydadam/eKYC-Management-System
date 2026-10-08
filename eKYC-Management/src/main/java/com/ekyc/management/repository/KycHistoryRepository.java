package com.ekyc.management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ekyc.management.model.KycHistory;

public interface KycHistoryRepository
        extends JpaRepository<KycHistory, Long> {

    List<KycHistory>
    findByCustomerIdOrderByChangedAtDesc(
            String customerId
    );
}