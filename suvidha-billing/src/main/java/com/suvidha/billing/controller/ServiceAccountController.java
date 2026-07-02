package com.suvidha.billing.controller;

import com.suvidha.billing.dto.response.ServiceAccountResponse;
import com.suvidha.billing.service.ServiceAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing/accounts/service")
public class ServiceAccountController {

    private final ServiceAccountService serviceAccountService;

    public ServiceAccountController(ServiceAccountService serviceAccountService) {
        this.serviceAccountService = serviceAccountService;
    }

    @PostMapping("/create")
    public ResponseEntity<ServiceAccountResponse> createAccount(@RequestParam String verificationRequestId) {
        return ResponseEntity.ok(serviceAccountService.createFromVerification(verificationRequestId));
    }

    @GetMapping("/{citizenId}")
    public ResponseEntity<List<ServiceAccountResponse>> listCitizenAccounts(@PathVariable String citizenId) {
        return ResponseEntity.ok(serviceAccountService.getAccountsByCitizen(citizenId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/details/{accountId}")
    public ResponseEntity<ServiceAccountResponse> getAccountDetails(@PathVariable String accountId) {
        return ResponseEntity.ok(serviceAccountService.getAccountDetails(accountId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountId}/deactivate")
    public ResponseEntity<ServiceAccountResponse> deactivateAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(serviceAccountService.deactivateAccount(accountId));
    }
}
