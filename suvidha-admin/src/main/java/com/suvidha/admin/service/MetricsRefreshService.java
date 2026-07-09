package com.suvidha.admin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class MetricsRefreshService {

    private static final Logger log = LoggerFactory.getLogger(MetricsRefreshService.class);

    @PersistenceContext
    private EntityManager em;

    private final RestTemplate restTemplate;
    private final String authServiceUrl;
    private final String billingServiceUrl;
    private final String grievanceServiceUrl;

    public MetricsRefreshService(
            @Value("${auth.service.url:http://suvidha-auth:8081}") String authServiceUrl,
            @Value("${billing.service.url:http://suvidha-billing:8082}") String billingServiceUrl,
            @Value("${grievance.service.url:http://suvidha-grievance:8083}") String grievanceServiceUrl) {
        this.authServiceUrl = authServiceUrl;
        this.billingServiceUrl = billingServiceUrl;
        this.grievanceServiceUrl = grievanceServiceUrl;
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void refreshMetrics() {
        try {
            upsertMetric("total_users", null, fetchCount(authServiceUrl + "/api/auth/health"), null);
            upsertMetric("total_grievances", null, fetchCount(grievanceServiceUrl + "/actuator/health"), null);
        } catch (Exception e) {
            log.warn("Failed to refresh metrics from upstream services: {}", e.getMessage());
        }
    }

    private long fetchCount(String url) {
        try {
            var response = restTemplate.getForEntity(url, Object.class);
            return response.getStatusCode().is2xxSuccessful() ? 1L : 0L;
        } catch (Exception e) {
            log.debug("Failed to fetch from {}: {}", url, e.getMessage());
            return 0L;
        }
    }

    private void upsertMetric(String metricType, String dimension, long totalCount, Double numericValue) {
        var existing = em.createNativeQuery(
            "SELECT id FROM admin.metrics_aggregate WHERE metric_type = :type AND (dimension = :dim OR (:dim IS NULL AND dimension IS NULL))")
            .setParameter("type", metricType)
            .setParameter("dim", dimension);
        existing.getResultList().forEach(id -> {
            em.createNativeQuery(
                "UPDATE admin.metrics_aggregate SET total_count = :count, numeric_value = :num, updated_at = :now WHERE id = :id")
                .setParameter("count", totalCount)
                .setParameter("num", numericValue)
                .setParameter("now", Instant.now())
                .setParameter("id", id)
                .executeUpdate();
        });
    }
}
