package com.suvidha.billing.controller;

import com.suvidha.billing.dto.request.NewConnectionRequestDto;
import com.suvidha.billing.dto.response.NewConnectionResponse;
import com.suvidha.billing.dto.response.ServiceAccountResponse;
import com.suvidha.billing.exception.UnauthorizedException;
import com.suvidha.billing.security.SecurityUtil;
import com.suvidha.billing.service.NewConnectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing/connections/new")
public class NewConnectionController {

    private final NewConnectionService newConnectionService;

    public NewConnectionController(NewConnectionService newConnectionService) {
        this.newConnectionService = newConnectionService;
    }

    @PostMapping("/request")
    public ResponseEntity<NewConnectionResponse> submitRequest(
            @Valid @RequestBody NewConnectionRequestDto dto) {
        // Identity always comes from the JWT principal — no header fallback
        String citizenId = SecurityUtil.currentCitizenId();
        return ResponseEntity.ok(newConnectionService.submitRequest(citizenId, dto));
    }

    @GetMapping("/status/{refNo}")
    public ResponseEntity<NewConnectionResponse> trackStatus(@PathVariable String refNo) {
        return ResponseEntity.ok(newConnectionService.getStatus(refNo));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/citizen/{citizenId}")
    public ResponseEntity<List<NewConnectionResponse>> getByCitizen(@PathVariable String citizenId) {
        String authenticatedCitizenId = SecurityUtil.currentCitizenId();
        if (!authenticatedCitizenId.equals(citizenId)) {
            throw new UnauthorizedException(
                    "Access denied: cannot view connections for another citizen");
        }
        return ResponseEntity.ok(newConnectionService.getRequestsByCitizen(citizenId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/approve/{requestId}")
    public ResponseEntity<NewConnectionResponse> approveRequest(
            @PathVariable String requestId,
            @RequestParam String providerName) {
        return ResponseEntity.ok(newConnectionService.approveRequest(requestId, providerName));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/reject/{requestId}")
    public ResponseEntity<NewConnectionResponse> rejectRequest(
            @PathVariable String requestId,
            @RequestParam String reason) {
        return ResponseEntity.ok(newConnectionService.rejectRequest(requestId, reason));
    }

    @PreAuthorize("hasRole('PROVIDER')")
    @PostMapping("/provider/complete/{requestId}")
    public ResponseEntity<ServiceAccountResponse> completeConnection(
            @PathVariable String requestId,
            @RequestParam(required = false) String accountNo) {
        return ResponseEntity.ok(newConnectionService.completeConnection(requestId, accountNo));
    }
}
