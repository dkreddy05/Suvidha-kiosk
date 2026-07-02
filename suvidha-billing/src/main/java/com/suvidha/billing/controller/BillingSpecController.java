package com.suvidha.billing.controller;

import com.suvidha.billing.dto.request.SpecLinkAccountRequest;
import com.suvidha.billing.dto.request.SpecPaymentRequest;
import com.suvidha.billing.dto.response.*;
import com.suvidha.billing.exception.InvalidRequestException;
import com.suvidha.billing.security.CitizenAuthDetails;
import com.suvidha.billing.security.SecurityUtil;
import com.suvidha.billing.service.BillingSpecService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Spec-compliant billing controller.
 *
 * <p>All 7 endpoints from the Billing Service spec are implemented here with
 * the exact paths, status codes, and response shapes required.
 *
 * <p>The legacy {@link BillingController} is preserved on {@code /api/billing}
 * for backward compatibility with the kiosk frontend.
 */
@RestController
@RequestMapping("/api/billing")
@PreAuthorize("isAuthenticated()")
public class BillingSpecController {

    private final BillingSpecService billingSpecService;

    public BillingSpecController(BillingSpecService billingSpecService) {
        this.billingSpecService = billingSpecService;
    }

    /**
     * POST /api/v1/billing/accounts — Link a new utility account.
     * Returns 201 Created.
     */
    @PostMapping("/accounts")
    public ResponseEntity<SpecAccountResponse> linkAccount(
            @Valid @RequestBody SpecLinkAccountRequest request) {
        String citizenId = SecurityUtil.currentCitizenId();
        String mobile = currentMobile();
        SpecAccountResponse response = billingSpecService.linkAccount(request, citizenId, mobile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/billing/accounts — List all linked accounts for the authenticated citizen.
     */
    @GetMapping("/accounts")
    public ResponseEntity<AccountsListResponse> getAccounts() {
        String citizenId = SecurityUtil.currentCitizenId();
        return ResponseEntity.ok(billingSpecService.getAccounts(citizenId));
    }

    /**
     * GET /api/v1/billing/accounts/{accountId}/bills — Get bills for an account.
     */
    @GetMapping("/accounts/{accountId}/bills")
    public ResponseEntity<SpecBillsResponse> getBills(@PathVariable String accountId) {
        String citizenId = SecurityUtil.currentCitizenId();
        return ResponseEntity.ok(billingSpecService.getBills(accountId, citizenId));
    }

    /**
     * GET /api/v1/billing/accounts/{accountId}/bills/{billId} — Get a specific bill.
     */
    @GetMapping("/accounts/{accountId}/bills/{billId}")
    public ResponseEntity<SpecBillResponse> getBillById(
            @PathVariable String accountId,
            @PathVariable String billId) {
        String citizenId = SecurityUtil.currentCitizenId();
        return ResponseEntity.ok(billingSpecService.getBillById(accountId, billId, citizenId));
    }

    /**
     * POST /api/v1/billing/accounts/{accountId}/payments — Process a payment.
     */
    @PostMapping("/accounts/{accountId}/payments")
    public ResponseEntity<SpecPaymentResponse> processPayment(
            @PathVariable String accountId,
            @Valid @RequestBody SpecPaymentRequest request) {
        String citizenId = SecurityUtil.currentCitizenId();
        return ResponseEntity.ok(billingSpecService.processPayment(accountId, request, citizenId));
    }

    /**
     * GET /api/v1/billing/accounts/{accountId}/payments — Get payment history (paginated).
     */
    @GetMapping("/accounts/{accountId}/payments")
    public ResponseEntity<PaymentHistoryResponse> getPaymentHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        String citizenId = SecurityUtil.currentCitizenId();
        return ResponseEntity.ok(billingSpecService.getPaymentHistory(accountId, citizenId, limit, offset));
    }

    /**
     * GET /api/v1/billing/accounts/{accountId}/payments/{transactionId}/receipt — Get receipt.
     */
    @GetMapping("/accounts/{accountId}/payments/{transactionId}/receipt")
    public ResponseEntity<SpecPaymentResponse> getReceipt(
            @PathVariable String accountId,
            @PathVariable String transactionId) {
        String citizenId = SecurityUtil.currentCitizenId();
        return ResponseEntity.ok(billingSpecService.getReceipt(accountId, transactionId, citizenId));
    }

    // ── helper ──────────────────────────────────────────────────────────────

    private String currentMobile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof CitizenAuthDetails d
                && d.getMobile() != null && !d.getMobile().isBlank()) {
            return d.getMobile();
        }
        throw new InvalidRequestException("Mobile number not available");
    }
}
