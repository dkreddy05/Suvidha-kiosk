package com.suvidha.billing.controller;

import com.suvidha.billing.dto.request.ConfirmPaymentRequest;
import com.suvidha.billing.dto.request.LinkAccountRequest;
import com.suvidha.billing.dto.request.PayBillRequest;
import com.suvidha.billing.dto.response.*;
import com.suvidha.billing.exception.InvalidRequestException;
import com.suvidha.billing.security.CitizenAuthDetails;
import com.suvidha.billing.security.SecurityUtil;
import com.suvidha.billing.service.BillingFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Primary billing controller — all paths rooted at /api/v1/billing
 * to match the kiosk frontend's axiosClient base (/api/v1) + endpoints.ts.
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingFacadeService billingFacadeService;

    public BillingController(BillingFacadeService billingFacadeService) {
        this.billingFacadeService = billingFacadeService;
    }

    /**
     * GET /api/v1/billing/accounts?mobile={mobile}
     * Returns all active utility accounts for the citizen, each with their latest bill.
     * Frontend: useAccounts(mobileRaw) hook in useBilling.ts
     */
    @GetMapping(value = "/accounts", params = "mobile")
    public ResponseEntity<List<UtilityAccountDTO>> getAccounts(
            @RequestParam String mobile,
            @RequestHeader(value = "X-Kiosk-Id", required = false) String kioskId) {
        return ResponseEntity.ok(billingFacadeService.getAccountsForMobile(mobile));
    }

    /**
     * GET /api/v1/billing/bills/{accountId}
     * Returns all bills for a specific account, ordered newest first.
     * Frontend: useBills(accountId) hook
     */
    @GetMapping("/bills/{accountId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BillDTO>> getBillsForAccount(
            @PathVariable String accountId,
            @RequestHeader(value = "X-Kiosk-Id", required = false) String kioskId) {
        String citizenId = SecurityUtil.currentCitizenId();
        return ResponseEntity.ok(billingFacadeService.getBillsForAccount(accountId, citizenId));
    }

    /**
     * GET /api/v1/billing/bill/{billId}
     * Returns a single bill by ID.
     * Frontend: useBill(billId) hook
     */
    @GetMapping("/bill/{billId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BillDTO> getBill(
            @PathVariable String billId) {
        String citizenId = SecurityUtil.currentCitizenId();
        return ResponseEntity.ok(billingFacadeService.getBillById(billId, citizenId));
    }

    /**
     * POST /api/v1/billing/pay
     * Initiates a payment for one or more bills.
     * Returns PaymentOrderDTO (UPI/CARD) or { status: "PAID" } (CASH).
     * Frontend: usePayBill() mutation in useBilling.ts
     */
    @PostMapping("/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> initiatePayment(
            @Valid @RequestBody PayBillRequest req,
            @RequestHeader(value = "X-Kiosk-Id", required = false) String kioskId) {
        String citizenId = SecurityUtil.currentCitizenId();
        Object result = billingFacadeService.initiatePay(req, citizenId);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/billing/pay/confirm
     * Confirms a Razorpay payment after the citizen completes UPI/card flow.
     * Frontend: useConfirmPayment() mutation with Idempotency-Key header
     */
    @PostMapping("/pay/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentConfirmDTO> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String citizenId = SecurityUtil.currentCitizenId();
        PaymentConfirmDTO result = billingFacadeService.confirmPayment(req, idempotencyKey, citizenId);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/billing/receipt/{paymentId}
     * Returns receipt HTML + JSON for a confirmed payment.
     * Frontend: useReceipt(confirmedPaymentId) hook in useBilling.ts
     */
    @GetMapping("/receipt/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReceiptDTO> getReceipt(@PathVariable String paymentId) {
        String citizenId = SecurityUtil.currentCitizenId();
        return ResponseEntity.ok(billingFacadeService.getReceipt(paymentId, citizenId));
    }

    /**
     * POST /api/v1/billing/accounts/link
     * Links (or creates) a utility account to the authenticated citizen.
     * Frontend: useLinkAccount() mutation in useBilling.ts
     */
    @PostMapping("/accounts/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UtilityAccountDTO> linkAccount(
            @Valid @RequestBody LinkAccountRequest req,
            @RequestHeader(value = "X-Kiosk-Id", required = false) String kioskId) {
        String citizenId = SecurityUtil.currentCitizenId();
        String mobile = currentMobile();
        UtilityAccountDTO result = billingFacadeService.linkAccount(req, citizenId, mobile);
        return ResponseEntity.ok(result);
    }

    // ── helper ────────────────────────────────────────────────────────────

    private String currentMobile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof CitizenAuthDetails d
                && d.getMobile() != null && !d.getMobile().isBlank()) {
            return d.getMobile();
        }
        throw new InvalidRequestException("Mobile number not available");
    }
}
