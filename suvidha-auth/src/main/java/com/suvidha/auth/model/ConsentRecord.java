package com.suvidha.auth.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_records", schema = "auth")
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "consent_id", length = 36)
    private String consentId;

    @Column(name = "citizen_id", length = 36, nullable = false)
    private String citizenId;

    @Column(name = "consent_type", nullable = false)
    private String consentType;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    public ConsentRecord() {}

    public ConsentRecord(String citizenId, String consentType, Instant grantedAt, Instant expiresAt, String ipAddress, String userAgent) {
        this.consentId = UUID.randomUUID().toString();
        this.citizenId = citizenId;
        this.consentType = consentType;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public String getConsentType() { return consentType; }
    public void setConsentType(String consentType) { this.consentType = consentType; }
    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
