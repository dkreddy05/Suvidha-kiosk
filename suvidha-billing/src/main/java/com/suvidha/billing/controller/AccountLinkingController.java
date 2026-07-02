package com.suvidha.billing.controller;

import com.suvidha.billing.dto.request.AccountVerificationRequestDto;
import com.suvidha.billing.dto.request.ConfirmLinkRequest;
import com.suvidha.billing.dto.request.VerifyOwnershipRequest;
import com.suvidha.billing.dto.response.ApiResponse;
import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.security.SecurityUtil;
import com.suvidha.billing.service.AccountLinkingService;
import com.suvidha.billing.service.KioskLogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core/utility/{serviceType}")
@PreAuthorize("isAuthenticated()")
public class AccountLinkingController {

    private final AccountLinkingService accountLinkingService;
    private final KioskLogService kioskLogService;

    public AccountLinkingController(AccountLinkingService accountLinkingService, KioskLogService kioskLogService) {
        this.accountLinkingService = accountLinkingService;
        this.kioskLogService = kioskLogService;
    }

    @PostMapping("/accounts/verify-ownership")
    public ApiResponse<?> verifyOwnership(
            @PathVariable String serviceType,
            @RequestHeader(value = "X-Kiosk-Id", required = false) String kioskId,
            @Valid @RequestBody VerifyOwnershipRequest req) {
        ServiceType st = SecurityUtil.parseServiceType(serviceType);
        if (req.getServiceType() != st) {
            throw new IllegalArgumentException("serviceType mismatch");
        }

        String citizenId = SecurityUtil.currentCitizenId();
        if (kioskId != null && !kioskId.isBlank()) {
            kioskLogService.log(kioskId, citizenId, "VERIFY_OWNERSHIP", "/accounts/verify-ownership", null);
        }
        return ApiResponse.success(accountLinkingService.verifyOwnership(citizenId, req));
    }

    @PostMapping("/accounts/confirm-link")
    public ApiResponse<?> confirmLink(
            @PathVariable String serviceType,
            @RequestHeader(value = "X-Kiosk-Id", required = false) String kioskId,
            @Valid @RequestBody ConfirmLinkRequest req) {
        ServiceType st = SecurityUtil.parseServiceType(serviceType);
        if (req.getServiceType() != st) {
            throw new IllegalArgumentException("serviceType mismatch");
        }

        String citizenId = SecurityUtil.currentCitizenId();
        if (kioskId != null && !kioskId.isBlank()) {
            kioskLogService.log(kioskId, citizenId, "CONFIRM_LINK", "/accounts/confirm-link", null);
        }
        return ApiResponse.success(accountLinkingService.confirmLink(citizenId, req));
    }

    @PostMapping("/accounts/request-verification")
    public ApiResponse<?> requestVerification(
            @PathVariable String serviceType,
            @RequestHeader(value = "X-Kiosk-Id", required = false) String kioskId,
            @Valid @RequestBody AccountVerificationRequestDto req) {
        ServiceType st = SecurityUtil.parseServiceType(serviceType);
        if (req.getServiceType() != st) {
            throw new IllegalArgumentException("serviceType mismatch");
        }

        String citizenId = SecurityUtil.currentCitizenId();
        Object res = accountLinkingService.requestVerification(citizenId, req);
        if (kioskId != null && !kioskId.isBlank()) {
            kioskLogService.log(kioskId, citizenId, "REQUEST_VERIFICATION", "/accounts/request-verification", null);
        }
        return ApiResponse.success(res);
    }

    @GetMapping("/accounts/verification-status/{refNo}")
    public ApiResponse<?> verificationStatus(
            @RequestHeader(value = "X-Kiosk-Id", required = false) String kioskId,
            @PathVariable String serviceType,
            @PathVariable String refNo) {
        String citizenId = SecurityUtil.currentCitizenId();
        if (kioskId != null && !kioskId.isBlank()) {
            kioskLogService.log(kioskId, citizenId, "GET_VERIFICATION_STATUS", "/accounts/verification-status/{refNo}",
                    refNo);
        }
        return ApiResponse.success(accountLinkingService.getVerificationStatus(refNo));
    }

    @GetMapping("/accounts/providers")
    public ApiResponse<?> providers(@PathVariable String serviceType) {
        ServiceType st = SecurityUtil.parseServiceType(serviceType);
        return ApiResponse.success(accountLinkingService.getProviders(st));
    }
}
