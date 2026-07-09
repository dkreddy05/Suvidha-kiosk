package com.suvidha.admin.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Repository
public class MetricsRepository {

    private static final Logger log = LoggerFactory.getLogger(MetricsRepository.class);

    @PersistenceContext
    private EntityManager em;

    public long countTotalUsers() {
        try {
            Query q = em.createNativeQuery("SELECT COALESCE(SUM(total_count), 0) FROM admin.metrics_aggregate WHERE metric_type = 'total_users'");
            return ((Number) q.getSingleResult()).longValue();
        } catch (Exception e) {
            log.warn("Failed to query total users from aggregate table: {}", e.getMessage());
            return 0L;
        }
    }

    public long countNewUsersSince(Instant since) {
        try {
            Query q = em.createNativeQuery("SELECT COALESCE(SUM(total_count), 0) FROM admin.metrics_aggregate WHERE metric_type = 'new_users' AND updated_at >= :since");
            q.setParameter("since", since);
            return ((Number) q.getSingleResult()).longValue();
        } catch (Exception e) {
            log.warn("Failed to query new users from aggregate table: {}", e.getMessage());
            return 0L;
        }
    }

    public long countTotalGrievances() {
        try {
            Query q = em.createNativeQuery("SELECT COALESCE(SUM(total_count), 0) FROM admin.metrics_aggregate WHERE metric_type = 'total_grievances'");
            return ((Number) q.getSingleResult()).longValue();
        } catch (Exception e) {
            log.warn("Failed to query total grievances from aggregate table: {}", e.getMessage());
            return 0L;
        }
    }

    public long countGrievancesByStatus(String status) {
        try {
            Query q = em.createNativeQuery(
                "SELECT COALESCE(SUM(total_count), 0) FROM admin.metrics_aggregate WHERE metric_type = 'grievance_status' AND dimension = :status");
            q.setParameter("status", status);
            return ((Number) q.getSingleResult()).longValue();
        } catch (Exception e) {
            log.warn("Failed to query grievances by status from aggregate table: {}", e.getMessage());
            return 0L;
        }
    }

    public Double avgGrievanceResolutionHours() {
        try {
            Query q = em.createNativeQuery(
                "SELECT COALESCE(AVG(numeric_value), 0) FROM admin.metrics_aggregate WHERE metric_type = 'avg_resolution_hours'");
            Number result = (Number) q.getSingleResult();
            return result != null ? result.doubleValue() : 0.0;
        } catch (Exception e) {
            log.warn("Failed to query avg resolution hours from aggregate table: {}", e.getMessage());
            return 0.0;
        }
    }

    public Map<String, Object> paymentStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            Query q = em.createNativeQuery(
                "SELECT " +
                "  COALESCE(SUM(CASE WHEN metric_type = 'payment_total_amount' THEN numeric_value ELSE 0 END), 0) AS total_amount, " +
                "  COALESCE(SUM(CASE WHEN metric_type = 'payment_transaction_count' THEN total_count ELSE 0 END), 0) AS transaction_count, " +
                "  COALESCE(SUM(CASE WHEN metric_type = 'payment_success_count' THEN total_count ELSE 0 END), 0) AS success_count, " +
                "  COALESCE(SUM(CASE WHEN metric_type = 'payment_failed_count' THEN total_count ELSE 0 END), 0) AS failed_count " +
                "FROM admin.metrics_aggregate WHERE metric_type LIKE 'payment_%'");
            Object[] row = (Object[]) q.getSingleResult();
            result.put("total_amount", row[0] != null ? ((Number) row[0]).doubleValue() : 0.0);
            result.put("transaction_count", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            result.put("success_count", row[2] != null ? ((Number) row[2]).longValue() : 0L);
            result.put("failed_count", row[3] != null ? ((Number) row[3]).longValue() : 0L);
        } catch (Exception e) {
            log.warn("Failed to query payment stats from aggregate table: {}", e.getMessage());
            result.put("total_amount", 0.0);
            result.put("transaction_count", 0L);
            result.put("success_count", 0L);
            result.put("failed_count", 0L);
        }
        return result;
    }

    public Map<String, Object> paymentStatsSince(Instant since) {
        Map<String, Object> result = new HashMap<>();
        try {
            Query q = em.createNativeQuery(
                "SELECT " +
                "  COALESCE(SUM(CASE WHEN metric_type = 'payment_total_amount' THEN numeric_value ELSE 0 END), 0) AS total_amount, " +
                "  COALESCE(SUM(CASE WHEN metric_type = 'payment_transaction_count' THEN total_count ELSE 0 END), 0) AS transaction_count, " +
                "  COALESCE(SUM(CASE WHEN metric_type = 'payment_success_count' THEN total_count ELSE 0 END), 0) AS success_count, " +
                "  COALESCE(SUM(CASE WHEN metric_type = 'payment_failed_count' THEN total_count ELSE 0 END), 0) AS failed_count " +
                "FROM admin.metrics_aggregate WHERE metric_type LIKE 'payment_%' AND updated_at >= :since");
            q.setParameter("since", since);
            Object[] row = (Object[]) q.getSingleResult();
            result.put("total_amount", row[0] != null ? ((Number) row[0]).doubleValue() : 0.0);
            result.put("transaction_count", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            result.put("success_count", row[2] != null ? ((Number) row[2]).longValue() : 0L);
            result.put("failed_count", row[3] != null ? ((Number) row[3]).longValue() : 0L);
        } catch (Exception e) {
            log.warn("Failed to query payment stats since from aggregate table: {}", e.getMessage());
            result.put("total_amount", 0.0);
            result.put("transaction_count", 0L);
            result.put("success_count", 0L);
            result.put("failed_count", 0L);
        }
        return result;
    }
}
