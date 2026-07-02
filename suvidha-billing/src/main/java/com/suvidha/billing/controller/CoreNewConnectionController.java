package com.suvidha.billing.controller;

import com.suvidha.billing.dto.request.NewConnectionRequestDto;
import com.suvidha.billing.dto.response.ApiResponse;
import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.security.SecurityUtil;
import com.suvidha.billing.service.KioskLogService;
import com.suvidha.billing.service.NewConnectionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core/utility/{serviceType}")
@PreAuthorize("isAuthenticated()")
public class CoreNewConnectionController {

    private final NewConnectionService newConnectionService;
    private final KioskLogService kioskLogService;

    public CoreNewConnectionController(NewConnectionService newConnectionService, KioskLogService kioskLogService) {
        this.newConnectionService = newConnectionService;
        this.kioskLogService = kioskLogService;
    }

    @PostMapping("/connections/apply")
    public ApiResponse<?> apply(
            @PathVariable String serviceType,
            @RequestHeader(value = "X-Kiosk-Id", required = false) String kioskId,
            @Valid @RequestBody NewConnectionRequestDto req) {
        ServiceType st = SecurityUtil.parseServiceType(serviceType);
        if (req.getServiceType() != st) {
            throw new IllegalArgumentException("serviceType mismatch");
        }
        String citizenId = SecurityUtil.currentCitizenId();
        Object res = newConnectionService.applyForConnection(citizenId, req);
        if (kioskId != null && !kioskId.isBlank()) {
            kioskLogService.log(kioskId, citizenId, "APPLY_CONNECTION", "/connections/apply", null);
        }
        return ApiResponse.success(res);
    }

    @GetMapping("/connections/track/{refNo}")
    public ApiResponse<?> track(
            @RequestHeader(value = "X-Kiosk-Id", required = false) String kioskId,
            @PathVariable String serviceType,
            @PathVariable String refNo) {
        String citizenId = SecurityUtil.currentCitizenId();
        if (kioskId != null && !kioskId.isBlank()) {
            kioskLogService.log(kioskId, citizenId, "TRACK_CONNECTION", "/connections/track/{refNo}", refNo);
        }
        return ApiResponse.success(newConnectionService.trackConnection(refNo));
    }

    @GetMapping("/connections/mine")
    public ApiResponse<?> mine(@PathVariable String serviceType) {
        SecurityUtil.parseServiceType(serviceType);
        return ApiResponse.success(newConnectionService.myConnections(SecurityUtil.currentCitizenId()));
    }
}
