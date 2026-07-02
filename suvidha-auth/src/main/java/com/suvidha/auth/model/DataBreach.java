package com.suvidha.auth.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "data_breaches", schema = "auth")
public class DataBreach {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "breach_id", length = 36)
    private String breachId;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "reported_at")
    private Instant reportedAt;

    @Column(name = "affected_count")
    private int affectedCount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String remediation;

    public DataBreach() {}

    public DataBreach(Instant detectedAt, int affectedCount, String description) {
        this.breachId = UUID.randomUUID().toString();
        this.detectedAt = detectedAt;
        this.affectedCount = affectedCount;
        this.description = description;
    }

    public String getBreachId() { return breachId; }
    public void setBreachId(String breachId) { this.breachId = breachId; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
    public int getAffectedCount() { return affectedCount; }
    public void setAffectedCount(int affectedCount) { this.affectedCount = affectedCount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRemediation() { return remediation; }
    public void setRemediation(String remediation) { this.remediation = remediation; }
}
