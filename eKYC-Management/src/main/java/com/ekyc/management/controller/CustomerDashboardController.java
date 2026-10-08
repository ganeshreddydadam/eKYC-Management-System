package com.ekyc.management.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ekyc.management.model.Customer;
import com.ekyc.management.model.CustomerRequest;
import com.ekyc.management.model.Ekyc;
import com.ekyc.management.repository.CustomerRepository;
import com.ekyc.management.repository.CustomerRequestRepository;
import com.ekyc.management.repository.EkycRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class CustomerDashboardController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EkycRepository ekycRepository;

    @Autowired
    private CustomerRequestRepository customerRequestRepository;


    // =====================================================
    // CUSTOMER DASHBOARD
    // GET /customer-dashboard
    // =====================================================

    @GetMapping("/customer-dashboard")
    public String customerDashboard(
            HttpSession session,
            Model model) {


        // =================================================
        // CHECK CUSTOMER LOGIN
        // =================================================

        Boolean loggedIn =
                (Boolean) session.getAttribute(
                        "loggedIn"
                );

        String customerId =
                (String) session.getAttribute(
                        "loggedInCustomerId"
                );


        if (loggedIn == null ||
                !loggedIn ||
                customerId == null) {

            return "redirect:/login";
        }


        // =================================================
        // FIND CUSTOMER
        // =================================================

        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElse(null);


        if (customer == null) {

            session.invalidate();

            return "redirect:/login";
        }


        // =================================================
        // FIND eKYC RECORDS
        // =================================================

        List<Ekyc> ekycList =
                ekycRepository
                        .findByCustomerIdOrderBySubmittedAtDesc(
                                customerId
                        );


        Ekyc ekyc = null;


        if (ekycList != null &&
                !ekycList.isEmpty()) {

            ekyc = ekycList.get(0);
        }


        // =================================================
        // CUSTOMER REQUESTS
        // =================================================

        List<CustomerRequest> requests =
                customerRequestRepository
                        .findByCustomerIdOrderByRequestDateDesc(
                                customerId
                        );


        // =================================================
        // GET LATEST CUSTOMER REQUEST
        // =================================================

        CustomerRequest request = null;

        if (requests != null &&
                !requests.isEmpty()) {

            request = requests.get(0);
        }


        // =================================================
        // SEND CUSTOMER TO HTML
        // =================================================

        model.addAttribute(
                "customer",
                customer
        );


        // =================================================
        // SEND eKYC DATA
        // =================================================

        model.addAttribute(
                "ekyc",
                ekyc
        );


        // =================================================
        // eKYC STATUS
        // =================================================

        if (ekyc == null) {

            model.addAttribute(
                    "ekycStatus",
                    "NOT_SUBMITTED"
            );

            model.addAttribute(
                    "submittedAt",
                    null
            );

        } else {

            String status =
                    ekyc.getStatus();


            if (status == null ||
                    status.trim().isEmpty()) {

                status = "PENDING";
            }


            model.addAttribute(
                    "ekycStatus",
                    status
            );

            model.addAttribute(
                    "submittedAt",
                    ekyc.getSubmittedAt()
            );
        }


        // =================================================
        // SEND CUSTOMER REQUESTS
        // =================================================

        model.addAttribute(
                "requests",
                requests
        );


        // =================================================
        // SEND LATEST REQUEST
        // =================================================

        model.addAttribute(
                "request",
                request
        );


        // =================================================
        // KYC UPDATE SUCCESS MESSAGE
        // =================================================

        String requestSuccess =
                (String) session.getAttribute(
                        "requestSuccess"
                );

        if (requestSuccess != null &&
                !requestSuccess.trim().isEmpty()) {

            model.addAttribute(
                    "requestSuccess",
                    requestSuccess
            );

            // Remove after displaying once
            session.removeAttribute(
                    "requestSuccess"
            );
        }


        // =================================================
        // OPEN DASHBOARD
        // =================================================

        return "customer-dashboard";
    }
    
}