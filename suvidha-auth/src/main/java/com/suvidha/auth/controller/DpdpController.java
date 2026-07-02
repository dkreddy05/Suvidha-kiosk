package com.suvidha.auth.controller;

import com.suvidha.auth.model.Citizen;
import com.suvidha.auth.model.ConsentRecord;
import com.suvidha.auth.repo.CitizenRepo;
import com.suvidha.auth.repo.ConsentRepo;
import com.suvidha.auth.service.AuditService;
import com.suvidha.auth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping({"/api/citizen", "/api/v1/citizen"})
public class DpdpController {

    private final CitizenRepo citizenRepo;
    private final ConsentRepo consentRepo;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokenService;

    public DpdpController(CitizenRepo citizenRepo, ConsentRepo consentRepo,
                          AuditService auditService, RefreshTokenService refreshTokenService) {
        this.citizenRepo = citizenRepo;
        this.consentRepo = consentRepo;
        this.auditService = auditService;
        this.refreshTokenService = refreshTokenService;
    }

    private String getCurrentCitizenId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getPersonalData() {
        String citizenId = getCurrentCitizenId();
        Citizen citizen = citizenRepo.findById(citizenId)
                .orElseThrow(() -> new NoSuchElementException("Citizen not found"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("citizen_id", citizen.getId());
        data.put("mobile", citizen.getMobile());
        data.put("name", citizen.getName());
        data.put("language", citizen.getLanguagePreference());
        data.put("role", citizen.getRole() != null ? citizen.getRole().name() : null);
        data.put("created_at", citizen.getCreatedAt());

        List<ConsentRecord> consents = consentRepo.findByCitizenId(citizenId);
        data.put("consents", consents.stream().map(c -> Map.of(
                "consent_type", c.getConsentType(),
                "granted_at", c.getGrantedAt(),
                "expires_at", c.getExpiresAt()
        )).toList());

        return ResponseEntity.ok(data);
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportData() {
        String citizenId = getCurrentCitizenId();
        Citizen citizen = citizenRepo.findById(citizenId)
                .orElseThrow(() -> new NoSuchElementException("Citizen not found"));

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exported_at", Instant.now());
        export.put("citizen_id", citizen.getId());
        export.put("mobile", citizen.getMobile());
        export.put("name", citizen.getName());
        export.put("language", citizen.getLanguagePreference());
        export.put("role", citizen.getRole() != null ? citizen.getRole().name() : null);
        export.put("created_at", citizen.getCreatedAt());

        return ResponseEntity.ok(export);
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(@RequestBody Map<String, String> updates,
                                                              HttpServletRequest request) {
        String citizenId = getCurrentCitizenId();
        Citizen citizen = citizenRepo.findById(citizenId)
                .orElseThrow(() -> new NoSuchElementException("Citizen not found"));

        if (updates.containsKey("name")) {
            String name = updates.get("name");
            if (name != null && !name.isBlank() && name.length() <= 100
                    && name.matches("^[a-zA-Z\\s'-]+$")) {
                citizen.setName(name);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid name format"));
            }
        }
        if (updates.containsKey("language")) {
            String lang = updates.get("language");
            if (lang != null && lang.matches("^(en|hi|te|ta)$")) {
                citizen.setLanguagePreference(lang);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Language must be en, hi, te, or ta"));
            }
        }
        citizenRepo.save(citizen);
        auditService.log("PROFILE_UPDATE", citizenId, "Profile updated", request);
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Map<String, Object>> deleteAccount(HttpServletRequest request) {
        String citizenId = getCurrentCitizenId();
        Citizen citizen = citizenRepo.findById(citizenId)
                .orElseThrow(() -> new NoSuchElementException("Citizen not found"));

        refreshTokenService.revokeAllUserTokens(citizenId);

        citizen.setName("DELETED_" + citizenId);
        citizen.setMobile("DELETED_" + citizenId);
        citizen.setAadhar(null);
        citizenRepo.save(citizen);

        auditService.log("ACCOUNT_DELETION", citizenId, "Account soft-deleted", request);

        return ResponseEntity.ok(Map.of(
                "status", "soft_deleted",
                "message", "Account scheduled for permanent deletion in 30 days",
                "deleted_at", Instant.now().toString()
        ));
    }

    @PostMapping("/consent")
    public ResponseEntity<Map<String, Object>> recordConsent(@RequestBody Map<String, String> body,
                                                              HttpServletRequest request) {
        String citizenId = getCurrentCitizenId();
        String consentType = body.get("consent_type");
        if (consentType == null || consentType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "consent_type is required"));
        }

        ConsentRecord consent = new ConsentRecord(
                citizenId,
                consentType,
                Instant.now(),
                Instant.now().plusSeconds(365 * 24 * 60 * 60L),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        consentRepo.save(consent);

        return ResponseEntity.ok(Map.of(
                "consent_id", consent.getConsentId(),
                "status", "granted",
                "consent_type", consentType,
                "granted_at", consent.getGrantedAt().toString()
        ));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String citizenId = getCurrentCitizenId();
        var pageResult = auditService.getLogs(citizenId, PageRequest.of(page, size));
        List<Map<String, Object>> logs = pageResult.getContent().stream()
                .map(log -> Map.<String, Object>of(
                        "action", log.getAction(),
                        "details", log.getDetails() != null ? log.getDetails() : "",
                        "ip_address", log.getIpAddress() != null ? log.getIpAddress() : "",
                        "created_at", log.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(Map.of(
                "logs", logs,
                "totalElements", pageResult.getTotalElements(),
                "totalPages", pageResult.getTotalPages(),
                "currentPage", pageResult.getNumber()
        ));
    }
}
