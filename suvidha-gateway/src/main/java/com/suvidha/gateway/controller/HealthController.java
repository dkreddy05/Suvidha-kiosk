package com.suvidha.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple liveness / readiness check for the Gateway service.
 * GET /health → { "status": "UP", "service": "suvidha-gateway" }
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status",  "UP",
            "service", "suvidha-gateway"
        );
    }
}
