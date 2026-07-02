package com.suvidha.admin.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Repository
public class MetricsRepository {

    @PersistenceContext
    private EntityManager em;

    public long countTotalUsers() {
        Query q = em.createNativeQuery("SELECT COUNT(*) FROM auth.citizens_table");
        return ((Number) q.getSingleResult()).longValue();
    }

    public long countNewUsersSince(Instant since) {
        Query q = em.createNativeQuery("SELECT COUNT(*) FROM auth.citizens_table WHERE created_at >= :since");
        q.setParameter("since", since);
        return ((Number) q.getSingleResult()).longValue();
    }

    public long countTotalGrievances() {
        Query q = em.createNativeQuery("SELECT COUNT(*) FROM grievance.grievances");
        return ((Number) q.getSingleResult()).longValue();
    }

    public long countGrievancesByStatus(String status) {
        Query q = em.createNativeQuery("SELECT COUNT(*) FROM grievance.grievances WHERE status = :status");
        q.setParameter("status", status);
        return ((Number) q.getSingleResult()).longValue();
    }

    public Double avgGrievanceResolutionHours() {
        Query q = em.createNativeQuery(
                "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (updated_at - submitted_at)) / 3600), 0) " +
                "FROM grievance.grievances WHERE status = 'RESOLVED' AND updated_at IS NOT NULL");
        Number result = (Number) q.getSingleResult();
        return result != null ? result.doubleValue() : 0.0;
    }

    public Map<String, Object> paymentStats() {
        Query q = em.createNativeQuery(
                "SELECT " +
                "  COALESCE(SUM(amount), 0) AS total_amount, " +
                "  COUNT(*) AS transaction_count, " +
                "  COALESCE(SUM(CASE WHEN status = 'COMPLETED' OR status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS success_count, " +
                "  COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failed_count " +
                "FROM billing.payments");
        Object[] row = (Object[]) q.getSingleResult();
        Map<String, Object> result = new HashMap<>();
        result.put("total_amount", row[0] != null ? ((Number) row[0]).doubleValue() : 0.0);
        result.put("transaction_count", row[1] != null ? ((Number) row[1]).longValue() : 0L);
        result.put("success_count", row[2] != null ? ((Number) row[2]).longValue() : 0L);
        result.put("failed_count", row[3] != null ? ((Number) row[3]).longValue() : 0L);
        return result;
    }

    public Map<String, Object> paymentStatsSince(Instant since) {
        Query q = em.createNativeQuery(
                "SELECT " +
                "  COALESCE(SUM(amount), 0) AS total_amount, " +
                "  COUNT(*) AS transaction_count, " +
                "  COALESCE(SUM(CASE WHEN status = 'COMPLETED' OR status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS success_count, " +
                "  COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failed_count " +
                "FROM billing.payments WHERE created_at >= :since");
        q.setParameter("since", since);
        Object[] row = (Object[]) q.getSingleResult();
        Map<String, Object> result = new HashMap<>();
        result.put("total_amount", row[0] != null ? ((Number) row[0]).doubleValue() : 0.0);
        result.put("transaction_count", row[1] != null ? ((Number) row[1]).longValue() : 0L);
        result.put("success_count", row[2] != null ? ((Number) row[2]).longValue() : 0L);
        result.put("failed_count", row[3] != null ? ((Number) row[3]).longValue() : 0L);
        return result;
    }
}
