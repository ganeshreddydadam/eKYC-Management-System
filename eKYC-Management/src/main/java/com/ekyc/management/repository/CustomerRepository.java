package com.ekyc.management.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ekyc.management.model.Customer;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerId(String customerId);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByMobile(String mobile);

    Optional<Customer> findByAadhaarNumber(String aadhaarNumber);

    Optional<Customer> findByPanNumber(String panNumber);
}