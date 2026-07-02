package com.suvidha.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_citizen_id", columnList = "citizen_id"),
    @Index(name = "idx_refresh_token_token", columnList = "token"),
    @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
})
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String citizenId;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean revoked;

    public RefreshToken() {
    }

    public RefreshToken(String citizenId, String token, Instant expiresAt) {
        this.citizenId = citizenId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.revoked = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}