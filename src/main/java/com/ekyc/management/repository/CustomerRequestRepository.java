package com.ekyc.management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ekyc.management.model.CustomerRequest;

public interface CustomerRequestRepository
        extends JpaRepository<CustomerRequest, Long> {

    List<CustomerRequest> findByStatus(String status);

    List<CustomerRequest> findByCustomerId(String customerId);

    List<CustomerRequest> findByCustomerIdOrderByRequestDateDesc(
            String customerId);
}