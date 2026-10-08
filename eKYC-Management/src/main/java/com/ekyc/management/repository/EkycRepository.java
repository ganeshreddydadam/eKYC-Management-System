package com.ekyc.management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ekyc.management.model.Ekyc;

@Repository
public interface EkycRepository
        extends JpaRepository<Ekyc, Long> {

    List<Ekyc> findByCustomerId(
            String customerId
    );

    List<Ekyc> findByCustomerIdOrderBySubmittedAtDesc(
            String customerId
    );
}