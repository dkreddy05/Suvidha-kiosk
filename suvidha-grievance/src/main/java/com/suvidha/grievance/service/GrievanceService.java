package com.suvidha.grievance.service;

import com.suvidha.grievance.dto.GrievanceCategoryResponse;
import com.suvidha.grievance.dto.GrievanceSubmitRequest;
import com.suvidha.grievance.dto.GrievanceSubmitResponse;
import com.suvidha.grievance.dto.GrievanceSummaryResponse;
import com.suvidha.grievance.dto.GrievanceTrackResponse;
import com.suvidha.grievance.dto.GrievanceTrackResponse.StatusHistoryEntry;
import com.suvidha.grievance.model.Grievance;
import com.suvidha.grievance.model.GrievanceUpdate;
import com.suvidha.grievance.repo.GrievanceRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GrievanceService {

    private static final List<String> CATEGORIES = List.of(
            "Water Supply", "Electricity", "Gas", "Billing", "Connection", "Other");

    private final GrievanceRepo grievanceRepo;

    public GrievanceService(GrievanceRepo grievanceRepo) {
        this.grievanceRepo = grievanceRepo;
    }

    @Transactional
    public GrievanceSubmitResponse submit(String citizenId, GrievanceSubmitRequest request) {
        if (!CATEGORIES.contains(request.category())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid category. Must be one of: " + CATEGORIES);
        }

        Instant now = Instant.now();
        String referenceNumber = "GRV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        Grievance grievance = new Grievance();
        grievance.setCitizenId(citizenId);
        grievance.setReferenceNumber(referenceNumber);
        grievance.setCategory(request.category());
        grievance.setDescription(request.description());
        grievance.setPhotoUrl(request.photoUrl());
        grievance.setStatus("SUBMITTED");
        grievance.setSubmittedAt(now);
        grievance.setUpdatedAt(now);

        GrievanceUpdate initialUpdate = new GrievanceUpdate("SUBMITTED", "Grievance submitted successfully", now);
        grievance.addUpdate(initialUpdate);

        grievanceRepo.save(grievance);

        return new GrievanceSubmitResponse(referenceNumber, now, "SUBMITTED");
    }

    @Transactional(readOnly = true)
    public GrievanceTrackResponse track(String referenceNumber) {
        Grievance grievance = grievanceRepo.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Grievance with reference " + referenceNumber + " not found"));

        return toTrackResponse(grievance);
    }

    @Transactional(readOnly = true)
    public GrievanceTrackResponse trackWithOwnership(String referenceNumber, String citizenId) {
        Grievance grievance = grievanceRepo.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Grievance with reference " + referenceNumber + " not found"));

        if (!grievance.getCitizenId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to view this grievance");
        }

        return toTrackResponse(grievance);
    }

    private GrievanceTrackResponse toTrackResponse(Grievance grievance) {
        List<StatusHistoryEntry> history = grievance.getUpdates().stream()
                .map(u -> new StatusHistoryEntry(u.getStatus(), u.getUpdatedAt(), u.getMessage()))
                .collect(Collectors.toList());

        return new GrievanceTrackResponse(
                grievance.getReferenceNumber(),
                grievance.getCategory(),
                grievance.getDescription(),
                grievance.getPhotoUrl(),
                grievance.getStatus(),
                history,
                grievance.getSubmittedAt(),
                grievance.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> myGrievances(String citizenId, int page, int size, String statusFilter) {
        if (size > 50) size = 50;
        Pageable pageable = PageRequest.of(page, size);

        Page<Grievance> grievancePage;
        if (statusFilter != null && !statusFilter.isBlank()) {
            grievancePage = grievanceRepo.findByCitizenIdAndStatusOrderBySubmittedAtDesc(
                    citizenId, statusFilter.toUpperCase(), pageable);
        } else {
            grievancePage = grievanceRepo.findByCitizenIdOrderBySubmittedAtDesc(citizenId, pageable);
        }

        List<GrievanceSummaryResponse> items = grievancePage.getContent().stream()
                .map(g -> new GrievanceSummaryResponse(
                        g.getReferenceNumber(),
                        g.getCategory(),
                        g.getStatus(),
                        g.getSubmittedAt()))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("grievances", items);
        result.put("page", grievancePage.getNumber());
        result.put("totalCount", grievancePage.getTotalElements());
        return result;
    }

    public GrievanceCategoryResponse categories() {
        return new GrievanceCategoryResponse(CATEGORIES);
    }
}
