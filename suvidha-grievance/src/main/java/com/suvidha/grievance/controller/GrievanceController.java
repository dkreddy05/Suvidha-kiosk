package com.suvidha.grievance.controller;

import com.suvidha.grievance.dto.GrievanceCategoryResponse;
import com.suvidha.grievance.dto.GrievanceSubmitRequest;
import com.suvidha.grievance.dto.GrievanceSubmitResponse;
import com.suvidha.grievance.dto.GrievanceSummaryResponse;
import com.suvidha.grievance.dto.GrievanceTrackResponse;
import com.suvidha.grievance.service.GrievanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/grievances")
public class GrievanceController {

    private final GrievanceService grievanceService;

    public GrievanceController(GrievanceService grievanceService) {
        this.grievanceService = grievanceService;
    }

    @PostMapping
    public ResponseEntity<GrievanceSubmitResponse> submit(@Valid @RequestBody GrievanceSubmitRequest request) {
        GrievanceSubmitResponse response = grievanceService.submit(currentCitizenId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{referenceNumber}")
    public ResponseEntity<GrievanceTrackResponse> track(@PathVariable String referenceNumber) {
        return ResponseEntity.ok(grievanceService.trackWithOwnership(referenceNumber, currentCitizenId()));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> myGrievances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        var result = grievanceService.myGrievances(currentCitizenId(), page, size, status);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/categories")
    public ResponseEntity<GrievanceCategoryResponse> categories() {
        return ResponseEntity.ok(grievanceService.categories());
    }

    private String currentCitizenId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Unauthenticated");
        }
        return authentication.getName();
    }
}
