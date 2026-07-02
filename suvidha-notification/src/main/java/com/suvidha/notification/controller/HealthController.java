package com.suvidha.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping({"/health", "/api/notifications/health", "/api/v1/notifications/health"})
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "suvidha-notification");
    }

    @GetMapping({"/health/ready", "/api/notifications/health/ready", "/api/v1/notifications/health/ready"})
    public ResponseEntity<Map<String, Object>> readiness() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                return ResponseEntity.ok(Map.of(
                        "status", "UP",
                        "database", "CONNECTED",
                        "service", "suvidha-notification"));
            }
        } catch (Exception e) {
            // fall through
        }
        return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "database", "DISCONNECTED",
                "service", "suvidha-notification"));
    }
}
