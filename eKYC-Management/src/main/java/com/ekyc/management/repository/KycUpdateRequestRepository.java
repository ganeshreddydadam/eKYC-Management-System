package com.ekyc.management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ekyc.management.model.KycUpdateRequest;

@Repository
public interface KycUpdateRequestRepository
        extends JpaRepository<KycUpdateRequest, Long> {

    List<KycUpdateRequest> findByCustomerIdOrderByRequestDateDesc(
            String customerId);

    List<KycUpdateRequest> findByStatusOrderByRequestDateDesc(
            String status);

    boolean existsByCustomerIdAndUpdateFieldAndStatus(
            String customerId,
            String updateField,
            String status);
}