package com.suvidha.auth.service;

import com.suvidha.auth.model.RefreshToken;
import com.suvidha.auth.repo.RefreshTokenRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepo refreshTokenRepo;
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    public RefreshTokenService(RefreshTokenRepo refreshTokenRepo) {
        this.refreshTokenRepo = refreshTokenRepo;
    }

    @Transactional
    public RefreshToken createRefreshToken(String citizenId) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(REFRESH_TOKEN_EXPIRY_DAYS * 24 * 60 * 60 * 1000L);
        
        RefreshToken refreshToken = new RefreshToken(citizenId, token, expiresAt);
        return refreshTokenRepo.save(refreshToken);
    }

    @Transactional
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            refreshTokenRepo.delete(refreshToken);
            throw new RuntimeException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        refreshToken.setRevoked(true);
        refreshTokenRepo.save(refreshToken);
    }

    @Transactional
    public void revokeAllUserTokens(String citizenId) {
        refreshTokenRepo.findByCitizenIdAndRevokedFalse(citizenId)
                .forEach(token -> {
                    token.setRevoked(true);
                    refreshTokenRepo.save(token);
                });
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String oldToken) {
        RefreshToken oldRefreshToken = verifyRefreshToken(oldToken);
        revokeRefreshToken(oldToken);
        return createRefreshToken(oldRefreshToken.getCitizenId());
    }
}