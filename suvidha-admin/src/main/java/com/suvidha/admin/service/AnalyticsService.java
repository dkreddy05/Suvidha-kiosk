package com.suvidha.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.admin.dto.*;
import com.suvidha.admin.repository.MetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final String CACHE_PREFIX = "admin:metrics:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final MetricsRepository metricsRepo;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AnalyticsService(MetricsRepository metricsRepo, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.metricsRepo = metricsRepo;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public MetricsResponse<DashboardData> dashboardMetrics(Instant startDate, Instant endDate) {
        String cacheKey = CACHE_PREFIX + "dashboard:" + (startDate != null ? startDate.toString() : "none");
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, objectMapper.getTypeFactory()
                        .constructParametricType(MetricsResponse.class, DashboardData.class));
            } catch (Exception e) {
                log.warn("Failed to deserialize cached dashboard metrics", e);
            }
        }

        long totalUsers = metricsRepo.countTotalUsers();
        long totalGrievances = metricsRepo.countTotalGrievances();
        long grievancesOpen = metricsRepo.countGrievancesByStatus("OPEN");
        long grievancesResolved = metricsRepo.countGrievancesByStatus("RESOLVED");
        long grievancesPending = metricsRepo.countGrievancesByStatus("PENDING");

        Map<String, Object> paymentStats = startDate != null
                ? metricsRepo.paymentStatsSince(startDate)
                : metricsRepo.paymentStats();

        double totalPayments = (double) paymentStats.get("total_amount");
        long paymentCount = (long) paymentStats.get("transaction_count");
        long successCount = (long) paymentStats.get("success_count");
        long failedCount = (long) paymentStats.get("failed_count");
        double successRate = paymentCount > 0 ? (double) successCount / paymentCount : 0.0;

        DashboardData data = new DashboardData(totalUsers, 0L, totalGrievances,
                grievancesOpen, grievancesResolved, grievancesPending,
                totalPayments, paymentCount, successRate);

        MetricsResponse<DashboardData> response = new MetricsResponse<>(data, "custom");
        cacheResponse(cacheKey, response);
        return response;
    }

    public MetricsResponse<UsersData> usersMetrics(String period) {
        String cacheKey = CACHE_PREFIX + "users:" + period;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, objectMapper.getTypeFactory()
                        .constructParametricType(MetricsResponse.class, UsersData.class));
            } catch (Exception e) {
                log.warn("Failed to deserialize cached users metrics", e);
            }
        }

        long totalRegistered = metricsRepo.countTotalUsers();
        Instant since = periodStart(period);
        long newUsers = since != null ? metricsRepo.countNewUsersSince(since) : 0L;
        long activeSessions = 0L;
        long inactiveUsers = totalRegistered - activeSessions;

        UsersData data = new UsersData(totalRegistered, newUsers, activeSessions, Math.max(inactiveUsers, 0));
        MetricsResponse<UsersData> response = new MetricsResponse<>(data, period);
        cacheResponse(cacheKey, response);
        return response;
    }

    public MetricsResponse<GrievancesData> grievancesMetrics(String period) {
        String cacheKey = CACHE_PREFIX + "grievances:" + period;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, objectMapper.getTypeFactory()
                        .constructParametricType(MetricsResponse.class, GrievancesData.class));
            } catch (Exception e) {
                log.warn("Failed to deserialize cached grievances metrics", e);
            }
        }

        long total = metricsRepo.countTotalGrievances();
        long open = metricsRepo.countGrievancesByStatus("OPEN");
        long resolved = metricsRepo.countGrievancesByStatus("RESOLVED");
        long pending = metricsRepo.countGrievancesByStatus("PENDING");
        double avgResolutionTimeHours = metricsRepo.avgGrievanceResolutionHours();

        GrievancesData data = new GrievancesData(total, open, resolved, pending, avgResolutionTimeHours);
        MetricsResponse<GrievancesData> response = new MetricsResponse<>(data, period);
        cacheResponse(cacheKey, response);
        return response;
    }

    public MetricsResponse<PaymentsData> paymentsMetrics(String period) {
        String cacheKey = CACHE_PREFIX + "payments:" + period;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, objectMapper.getTypeFactory()
                        .constructParametricType(MetricsResponse.class, PaymentsData.class));
            } catch (Exception e) {
                log.warn("Failed to deserialize cached payments metrics", e);
            }
        }

        Instant since = periodStart(period);
        Map<String, Object> stats = since != null ? metricsRepo.paymentStatsSince(since) : metricsRepo.paymentStats();

        double totalAmount = (double) stats.get("total_amount");
        long transactionCount = (long) stats.get("transaction_count");
        long successCount = (long) stats.get("success_count");
        long failedCount = (long) stats.get("failed_count");
        double successRate = transactionCount > 0 ? (double) successCount / transactionCount : 0.0;
        double avgTransactionAmount = transactionCount > 0 ? totalAmount / transactionCount : 0.0;

        PaymentsData data = new PaymentsData(totalAmount, transactionCount, successCount,
                failedCount, successRate, avgTransactionAmount);
        MetricsResponse<PaymentsData> response = new MetricsResponse<>(data, period);
        cacheResponse(cacheKey, response);
        return response;
    }

    private void cacheResponse(String key, Object response) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache metrics response", e);
        }
    }

    private Instant periodStart(String period) {
        if (period == null) return null;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return switch (period.toLowerCase()) {
            case "day" -> today.atStartOfDay(ZoneOffset.UTC).toInstant();
            case "week" -> today.minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();
            case "month" -> today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            case "year" -> today.withDayOfYear(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            default -> today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        };
    }
}
