package com.suvidha.auth.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "jwt_key_versions")
public class JwtKeyVersion {
    @Id
    @Column(name = "kid", length = 36)
    private String kid;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "private_key", columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public JwtKeyVersion() {}

    public JwtKeyVersion(String kid, String publicKey, String privateKey, boolean isActive, Instant createdAt, Instant expiresAt) {
        this.kid = kid;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getKid() { return kid; }
    public void setKid(String kid) { this.kid = kid; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
