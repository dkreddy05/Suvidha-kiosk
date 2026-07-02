package com.suvidha.connections.controller;

import com.suvidha.connections.dto.ConnectionRequestCreateRequest;
import com.suvidha.connections.dto.ConnectionRequestCreateResponse;
import com.suvidha.connections.dto.ConnectionRequestSummaryResponse;
import com.suvidha.connections.dto.ConnectionStatusResponse;
import com.suvidha.connections.dto.ConnectionStatusUpdateRequest;
import com.suvidha.connections.service.ConnectionRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/connections/requests")
public class ConnectionRequestController {

    private final ConnectionRequestService connectionRequestService;

    public ConnectionRequestController(ConnectionRequestService connectionRequestService) {
        this.connectionRequestService = connectionRequestService;
    }

    @PostMapping
    public ResponseEntity<ConnectionRequestCreateResponse> create(@Valid @RequestBody ConnectionRequestCreateRequest request) {
        ConnectionRequestCreateResponse response = connectionRequestService.create(currentCitizenId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ConnectionRequestSummaryResponse>> myRequests() {
        return ResponseEntity.ok(connectionRequestService.myRequests(currentCitizenId()));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ConnectionStatusResponse> status(@PathVariable String requestId) {
        return ResponseEntity.ok(connectionRequestService.status(requestId, currentCitizenId()));
    }

    @PatchMapping("/{requestId}/status")
    public ResponseEntity<ConnectionStatusResponse> updateStatus(
            @PathVariable String requestId,
            @Valid @RequestBody ConnectionStatusUpdateRequest update) {
        return ResponseEntity.ok(connectionRequestService.updateStatus(requestId, currentCitizenId(), update));
    }

    private String currentCitizenId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Missing authenticated citizen");
        }
        return authentication.getName();
    }
}
