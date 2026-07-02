package com.suvidha.admin.controller;

import com.suvidha.admin.dto.*;
import com.suvidha.admin.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping({"/api/admin/metrics", "/api/v1/admin/metrics"})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<MetricsResponse<DashboardData>> dashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Instant start = parseDate(startDate);
        Instant end = parseDate(endDate);
        return ResponseEntity.ok(analyticsService.dashboardMetrics(start, end));
    }

    @GetMapping("/users")
    public ResponseEntity<MetricsResponse<UsersData>> users(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(analyticsService.usersMetrics(period));
    }

    @GetMapping("/grievances")
    public ResponseEntity<MetricsResponse<GrievancesData>> grievances(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(analyticsService.grievancesMetrics(period));
    }

    @GetMapping("/payments")
    public ResponseEntity<MetricsResponse<PaymentsData>> payments(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(analyticsService.paymentsMetrics(period));
    }

    private Instant parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return Instant.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use ISO 8601 (e.g. 2025-01-15T10:30:00Z)");
        }
    }
}
