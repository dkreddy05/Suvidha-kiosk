package com.suvidha.billing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Spring Boot HealthIndicator that continuously validates the Razorpay secret
 * is present and non-blank.
 *
 * <p>While {@code @PostConstruct} in BillingFacadeServiceImpl prevents the
 * application from starting without the secret, this health indicator provides
 * ongoing visibility through {@code /actuator/health} — useful for:
 * <ul>
 *     <li>Kubernetes liveness/readiness probes</li>
 *     <li>Monitoring dashboards</li>
 *     <li>Detecting secret rotation failures at runtime</li>
 * </ul>
 */
@Component
public class RazorpaySecretHealthIndicator implements HealthIndicator {

    @Value("${razorpay.secret:}")
    private String razorpaySecret;

    @Override
    public Health health() {
        if (razorpaySecret == null || razorpaySecret.isBlank()) {
            return Health.down()
                    .withDetail("reason", "RAZORPAY_SECRET is not configured")
                    .withDetail("impact", "Payment signature verification is DISABLED — all payments will be rejected")
                    .withDetail("action", "Set the RAZORPAY_SECRET environment variable or razorpay.secret property")
                    .build();
        }
        return Health.up()
                .withDetail("status", "Razorpay signature verification is active")
                .build();
    }
}
