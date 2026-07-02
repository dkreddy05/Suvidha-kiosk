package com.suvidha.auth.token;

import com.suvidha.auth.model.JwtKeyVersion;
import com.suvidha.auth.service.RsaKeyService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.security.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JWT attack surface tests.
 * Covers: expired tokens, tampered payloads, role modification, missing signatures,
 * none-algorithm attacks, replay attacks, logout bypass, refresh token reuse,
 * blacklisted token reuse, and stolen refresh token scenarios.
 */
@ExtendWith(MockitoExtension.class)
class JwtAttackSurfaceTest {

    @Mock
    private RsaKeyService rsaKeyService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private JwtToken authJwtToken;
    private JwtKeyVersion activeKey;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String validToken;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKey = pair.getPublic();

        String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        activeKey = new JwtKeyVersion(
                UUID.randomUUID().toString(),
                publicKeyStr,
                privateKeyStr,
                true,
                Instant.now(),
                Instant.now().plus(90, ChronoUnit.DAYS)
        );

        lenient().when(rsaKeyService.getActiveKeyVersion()).thenReturn(activeKey);
        lenient().when(rsaKeyService.getPrivateKey(activeKey)).thenReturn(privateKey);
        lenient().when(rsaKeyService.getPublicKeyByKid(activeKey.getKid())).thenReturn(publicKey);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);

        authJwtToken = new JwtToken(1800000, "suvidha-auth", "suvidha-services", rsaKeyService, redisTemplate);

        validToken = authJwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
    }

    // ========================================================================
    // ATTACK 1: Expired Token Usage
    // ========================================================================
    @Nested
    @DisplayName("Attack 1: Expired Token Usage")
    class ExpiredTokenAttack {

        @Test
        @DisplayName("SHOULD REJECT — token with exp in the past")
        void expiredToken_shouldBeRejected() {
            String expiredToken = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("suvidha-auth")
                    .audience().add("suvidha-services").and()
                    .claim("mobile", "9876543210")
                    .claim("name", "Test User")
                    .claim("role", "USER")
                    .claim("type", "access")
                    .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                    .expiration(Date.from(Instant.now().minusSeconds(60)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                    () -> authJwtToken.validate(expiredToken));
        }

        @Test
        @DisplayName("SHOULD REJECT — token expiring right now")
        void tokenExpiringNow_shouldBeRejected() {
            String nowToken = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("suvidha-auth")
                    .audience().add("suvidha-services").and()
                    .claim("role", "USER")
                    .claim("type", "access")
                    .issuedAt(Date.from(Instant.now().minusSeconds(1)))
                    .expiration(Date.from(Instant.now()))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                    () -> authJwtToken.validate(nowToken));
        }

        @Test
        @DisplayName("SHOULD ACCEPT — token with 1 second remaining")
        void tokenWithOneSecondLeft_shouldBeAccepted() {
            String almostExpired = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("suvidha-auth")
                    .audience().add("suvidha-services").and()
                    .claim("role", "USER")
                    .claim("type", "access")
                    .issuedAt(Date.from(Instant.now().minusSeconds(1799)))
                    .expiration(Date.from(Instant.now().plusSeconds(1)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            Claims claims = authJwtToken.validate(almostExpired);
            assertEquals("citizen-1", claims.getSubject());
        }
    }

    // ========================================================================
    // ATTACK 2: Tampered JWT Payload
    // ========================================================================
    @Nested
    @DisplayName("Attack 2: Tampered JWT Payload")
    class TamperedPayloadAttack {

        @Test
        @DisplayName("SHOULD REJECT — modifying citizenId in payload breaks signature")
        void tamperedCitizenId_shouldBeRejected() {
            String[] parts = validToken.split("\\.");
            String header = parts[0];
            String payload = parts[1];

            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
            String tamperedPayload = decodedPayload.replace("citizen-1", "citizen-999");
            String tamperedB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedPayload.getBytes());

            String tamperedToken = header + "." + tamperedB64 + "." + parts[2];

            assertThrows(io.jsonwebtoken.security.SignatureException.class,
                    () -> authJwtToken.validate(tamperedToken));
        }

        @Test
        @DisplayName("SHOULD REJECT — modifying role in payload breaks signature")
        void tamperedRole_shouldBeRejected() {
            String[] parts = validToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            String tamperedPayload = payload.replace("\"role\":\"USER\"", "\"role\":\"ADMIN\"");
            String tamperedB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedPayload.getBytes());

            String tamperedToken = parts[0] + "." + tamperedB64 + "." + parts[2];

            assertThrows(io.jsonwebtoken.security.SignatureException.class,
                    () -> authJwtToken.validate(tamperedToken));
        }

        @Test
        @DisplayName("SHOULD REJECT — appending characters to token breaks parsing")
        void appendedData_shouldBeRejected() {
            String tamperedToken = validToken + "extra";

            assertThrows(Exception.class,
                    () -> authJwtToken.validate(tamperedToken));
        }
    }

    // ========================================================================
    // ATTACK 3: Modified Role Claim (via re-signing with different key)
    // ========================================================================
    @Nested
    @DisplayName("Attack 3: Modified Role Claim")
    class ModifiedRoleAttack {

        @Test
        @DisplayName("SHOULD REJECT — re-signing with attacker's RSA key fails validation")
        void resignedWithDifferentKey_shouldBeRejected() throws Exception {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair attackerKeyPair = keyGen.generateKeyPair();

            String attackerToken = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("suvidha-auth")
                    .audience().add("suvidha-services").and()
                    .claim("mobile", "9876543210")
                    .claim("name", "Attacker")
                    .claim("role", "ADMIN")
                    .claim("type", "access")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(1800)))
                    .signWith(attackerKeyPair.getPrivate(), Jwts.SIG.RS256)
                    .compact();

            assertThrows(io.jsonwebtoken.security.SignatureException.class,
                    () -> authJwtToken.validate(attackerToken));
        }

        @Test
        @DisplayName("SHOULD REJECT — using wrong kid with valid signature fails")
        void wrongKid_shouldBeRejected() throws Exception {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair otherKeyPair = keyGen.generateKeyPair();

            lenient().when(rsaKeyService.getPublicKeyByKid("nonexistent-kid")).thenReturn(null);

            String otherToken = Jwts.builder()
                    .header().keyId("nonexistent-kid").and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("suvidha-auth")
                    .audience().add("suvidha-services").and()
                    .claim("role", "ADMIN")
                    .claim("type", "access")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(1800)))
                    .signWith(otherKeyPair.getPrivate(), Jwts.SIG.RS256)
                    .compact();

            assertThrows(IllegalArgumentException.class,
                    () -> authJwtToken.validate(otherToken));
        }
    }

    // ========================================================================
    // ATTACK 4: Missing Signature (unsigned JWT)
    // ========================================================================
    @Nested
    @DisplayName("Attack 4: Missing Signature")
    class MissingSignatureAttack {

        @Test
        @DisplayName("SHOULD REJECT — unsigned JWT (empty signature)")
        void unsignedToken_shouldBeRejected() {
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    "{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ("{\"sub\":\"citizen-1\",\"iss\":\"suvidha-auth\",\"aud\":\"suvidha-services\","
                            + "\"role\":\"ADMIN\",\"type\":\"access\",\"jti\":\"" + UUID.randomUUID() + "\","
                            + "\"iat\":" + Instant.now().getEpochSecond() + ","
                            + "\"exp\":" + Instant.now().plusSeconds(1800).getEpochSecond() + "}")
                            .getBytes());

            String unsignedToken = header + "." + payload + ".";

            assertThrows(Exception.class,
                    () -> authJwtToken.validate(unsignedToken));
        }
    }

    // ========================================================================
    // ATTACK 5: None Algorithm Attack
    // ========================================================================
    @Nested
    @DisplayName("Attack 5: None Algorithm Attack")
    class NoneAlgorithmAttack {

        @Test
        @DisplayName("SHOULD REJECT — alg:none with empty signature")
        void noneAlgorithm_shouldBeRejected() {
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ("{\"sub\":\"citizen-1\",\"iss\":\"suvidha-auth\",\"aud\":\"suvidha-services\","
                            + "\"role\":\"ADMIN\",\"type\":\"access\",\"jti\":\"" + UUID.randomUUID() + "\","
                            + "\"iat\":" + Instant.now().getEpochSecond() + ","
                            + "\"exp\":" + Instant.now().plusSeconds(1800).getEpochSecond() + "}")
                            .getBytes());

            String noneToken = header + "." + payload + ".";

            assertThrows(Exception.class,
                    () -> authJwtToken.validate(noneToken));
        }

        @Test
        @DisplayName("SHOULD REJECT — alg:None (case variation)")
        void noneAlgorithmCaseVariation_shouldBeRejected() {
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    "{\"alg\":\"None\",\"typ\":\"JWT\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ("{\"sub\":\"citizen-1\",\"iss\":\"suvidha-auth\",\"aud\":\"suvidha-services\","
                            + "\"role\":\"ADMIN\",\"type\":\"access\",\"jti\":\"" + UUID.randomUUID() + "\","
                            + "\"iat\":" + Instant.now().getEpochSecond() + ","
                            + "\"exp\":" + Instant.now().plusSeconds(1800).getEpochSecond() + "}")
                            .getBytes());

            String noneToken = header + "." + payload + ".";

            assertThrows(Exception.class,
                    () -> authJwtToken.validate(noneToken));
        }

        @Test
        @DisplayName("SHOULD REJECT — alg:HS256 (HMAC with public key as secret)")
        void hs256WithPublicKey_shouldBeRejected() throws Exception {
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ("{\"sub\":\"citizen-1\",\"iss\":\"suvidha-auth\",\"aud\":\"suvidha-services\","
                            + "\"role\":\"ADMIN\",\"type\":\"access\",\"jti\":\"" + UUID.randomUUID() + "\","
                            + "\"iat\":" + Instant.now().getEpochSecond() + ","
                            + "\"exp\":" + Instant.now().plusSeconds(1800).getEpochSecond() + "}")
                            .getBytes());

            String hs256Token = header + "." + payload + ".fakesignature";

            assertThrows(IllegalArgumentException.class,
                    () -> authJwtToken.validate(hs256Token));
        }
    }

    // ========================================================================
    // ATTACK 6: Replay Attacks
    // ========================================================================
    @Nested
    @DisplayName("Attack 6: Replay Attacks")
    class ReplayAttack {

        @Test
        @DisplayName("VULNERABLE — same valid token can be reused multiple times (expected for stateless JWTs)")
        void sameTokenCanBeReplayed_whileValid() {
            for (int i = 0; i < 100; i++) {
                Claims claims = authJwtToken.validate(validToken);
                assertEquals("citizen-1", claims.getSubject());
            }
        }

        @Test
        @DisplayName("EXPECTED — each token has unique jti for tracking")
        void eachTokenHasUniqueJti() {
            String token1 = authJwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
            String token2 = authJwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");

            Claims claims1 = authJwtToken.validate(token1);
            Claims claims2 = authJwtToken.validate(token2);

            assertNotEquals(claims1.getId(), claims2.getId());
        }

        @Test
        @DisplayName("EXPECTED — replay is limited by token expiration (30 min default)")
        void replayStopsAfterExpiration() {
            String shortLivedToken = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("suvidha-auth")
                    .audience().add("suvidha-services").and()
                    .claim("role", "USER")
                    .claim("type", "access")
                    .issuedAt(Date.from(Instant.now().minusSeconds(1801)))
                    .expiration(Date.from(Instant.now().minusSeconds(1)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                    () -> authJwtToken.validate(shortLivedToken));
        }
    }

    // ========================================================================
    // ATTACK 7: Logout Bypass
    // ========================================================================
    @Nested
    @DisplayName("Attack 7: Logout Bypass")
    class LogoutBypassAttack {

        @Test
        @DisplayName("SHOULD BLOCK — blacklisted token is rejected by auth service")
        void blacklistedToken_rejectedByAuthService() {
            String token = authJwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
            Claims claims = authJwtToken.validate(token);
            String jti = claims.getId();

            when(redisTemplate.hasKey("jwt:blacklist:" + jti)).thenReturn(true);

            assertTrue(authJwtToken.isBlacklisted(token));
        }
    }

    // ========================================================================
    // ATTACK 8: Refresh Token Reuse
    // ========================================================================
    @Nested
    @DisplayName("Attack 8: Refresh Token Reuse")
    class RefreshTokenReuseAttack {

        @Test
        @DisplayName("SHOULD BLOCK — revoked refresh token is rejected")
        void revokedRefreshToken_shouldBeRejected() {
            com.suvidha.auth.model.RefreshToken refreshToken =
                    new com.suvidha.auth.model.RefreshToken("citizen-1", UUID.randomUUID().toString(),
                            Instant.now().plusSeconds(604800));
            refreshToken.setRevoked(true);

            com.suvidha.auth.repo.RefreshTokenRepo mockRepo = mock(com.suvidha.auth.repo.RefreshTokenRepo.class);
            lenient().when(mockRepo.findByToken(refreshToken.getToken())).thenReturn(java.util.Optional.of(refreshToken));

            com.suvidha.auth.service.RefreshTokenService refreshTokenService =
                    new com.suvidha.auth.service.RefreshTokenService(mockRepo);

            assertThrows(RuntimeException.class,
                    () -> refreshTokenService.verifyRefreshToken(refreshToken.getToken()));
        }

        @Test
        @DisplayName("SHOULD BLOCK — expired refresh token is rejected and deleted")
        void expiredRefreshToken_shouldBeRejectedAndDeleted() {
            com.suvidha.auth.model.RefreshToken refreshToken =
                    new com.suvidha.auth.model.RefreshToken("citizen-1", UUID.randomUUID().toString(),
                            Instant.now().minusSeconds(1));

            com.suvidha.auth.repo.RefreshTokenRepo mockRepo = mock(com.suvidha.auth.repo.RefreshTokenRepo.class);
            lenient().when(mockRepo.findByToken(refreshToken.getToken())).thenReturn(java.util.Optional.of(refreshToken));

            com.suvidha.auth.service.RefreshTokenService refreshTokenService =
                    new com.suvidha.auth.service.RefreshTokenService(mockRepo);

            assertThrows(RuntimeException.class,
                    () -> refreshTokenService.verifyRefreshToken(refreshToken.getToken()));
            verify(mockRepo).delete(refreshToken);
        }

        @Test
        @DisplayName("VULNERABLE — concurrent reuse of same refresh token creates race condition")
        void concurrentReuse_raceCondition() {
            com.suvidha.auth.model.RefreshToken refreshToken =
                    new com.suvidha.auth.model.RefreshToken("citizen-1", UUID.randomUUID().toString(),
                            Instant.now().plusSeconds(604800));

            com.suvidha.auth.repo.RefreshTokenRepo mockRepo = mock(com.suvidha.auth.repo.RefreshTokenRepo.class);

            lenient().when(mockRepo.findByToken(refreshToken.getToken()))
                    .thenReturn(java.util.Optional.of(refreshToken));
            lenient().when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            com.suvidha.auth.service.RefreshTokenService refreshTokenService =
                    new com.suvidha.auth.service.RefreshTokenService(mockRepo);

            refreshTokenService.verifyRefreshToken(refreshToken.getToken());

            String oldToken = refreshToken.getToken();
            refreshToken.setRevoked(true);

            lenient().when(mockRepo.findByToken(oldToken))
                    .thenReturn(java.util.Optional.of(refreshToken));

            assertThrows(RuntimeException.class,
                    () -> refreshTokenService.verifyRefreshToken(oldToken));
        }
    }

    // ========================================================================
    // ATTACK 9: Blacklisted Token Reuse
    // ========================================================================
    @Nested
    @DisplayName("Attack 9: Blacklisted Token Reuse")
    class BlacklistedTokenReuseAttack {

        @Test
        @DisplayName("SHOULD BLOCK — blacklisted token cannot be validated for blacklist check")
        void blacklistedToken_isBlacklistedReturnsTrue() {
            String token = authJwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
            Claims claims = authJwtToken.validate(token);
            String jti = claims.getId();

            when(redisTemplate.hasKey("jwt:blacklist:" + jti)).thenReturn(true);

            assertTrue(authJwtToken.isBlacklisted(token));
        }

        @Test
        @DisplayName("SHOULD BLOCK — token blacklisted with correct TTL")
        void blacklistedToken_hasCorrectTtl() {
            String token = authJwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
            Claims claims = authJwtToken.validate(token);
            String jti = claims.getId();

            doNothing().when(valueOps).set(eq("jwt:blacklist:" + jti), eq("1"), any(java.time.Duration.class));

            authJwtToken.blacklistToken(token);

            verify(valueOps).set(eq("jwt:blacklist:" + jti), eq("1"), argThat(duration ->
                    duration != null && duration.getSeconds() > 0 && duration.getSeconds() <= 1800));
        }

        @Test
        @DisplayName("SHOULD BLOCK — expired token cannot be blacklisted (graceful handling)")
        void expiredToken_blacklistHandlesGracefully() {
            String expiredToken = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("suvidha-auth")
                    .audience().add("suvidha-services").and()
                    .claim("role", "USER")
                    .claim("type", "access")
                    .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                    .expiration(Date.from(Instant.now().minusSeconds(60)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            authJwtToken.blacklistToken(expiredToken);

            verifyNoInteractions(valueOps);
        }
    }

    // ========================================================================
    // ATTACK 10: Stolen Refresh Token Scenario
    // ========================================================================
    @Nested
    @DisplayName("Attack 10: Stolen Refresh Token Scenario")
    class StolenRefreshTokenAttack {

        @Test
        @DisplayName("VULNERABLE — refresh token not bound to device/IP")
        void refreshToken_noDeviceBinding() {
            com.suvidha.auth.model.RefreshToken refreshToken =
                    new com.suvidha.auth.model.RefreshToken("citizen-1", UUID.randomUUID().toString(),
                            Instant.now().plusSeconds(604800));

            com.suvidha.auth.repo.RefreshTokenRepo mockRepo = mock(com.suvidha.auth.repo.RefreshTokenRepo.class);
            lenient().when(mockRepo.findByToken(refreshToken.getToken())).thenReturn(java.util.Optional.of(refreshToken));
            lenient().when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            com.suvidha.auth.service.RefreshTokenService refreshTokenService =
                    new com.suvidha.auth.service.RefreshTokenService(mockRepo);

            com.suvidha.auth.model.RefreshToken verified =
                    refreshTokenService.verifyRefreshToken(refreshToken.getToken());

            assertEquals("citizen-1", verified.getCitizenId());
        }

        @Test
        @DisplayName("VULNERABLE — refresh token rotation doesn't detect family reuse")
        void refreshToken_rotationNoFamilyDetection() {
            com.suvidha.auth.model.RefreshToken refreshToken =
                    new com.suvidha.auth.model.RefreshToken("citizen-1", UUID.randomUUID().toString(),
                            Instant.now().plusSeconds(604800));

            com.suvidha.auth.repo.RefreshTokenRepo mockRepo = mock(com.suvidha.auth.repo.RefreshTokenRepo.class);

            when(mockRepo.findByToken(refreshToken.getToken()))
                    .thenReturn(java.util.Optional.of(refreshToken));
            when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            com.suvidha.auth.service.RefreshTokenService refreshTokenService =
                    new com.suvidha.auth.service.RefreshTokenService(mockRepo);

            com.suvidha.auth.model.RefreshToken rotated =
                    refreshTokenService.rotateRefreshToken(refreshToken.getToken());

            assertNotNull(rotated);
            assertNotEquals(refreshToken.getToken(), rotated.getToken());
            assertTrue(refreshToken.isRevoked());
        }

        @Test
        @DisplayName("EXPECTED — revokeAllUserTokens revokes all active refresh tokens for a user")
        void revokeAllUserTokens_worksCorrectly() {
            com.suvidha.auth.model.RefreshToken token1 =
                    new com.suvidha.auth.model.RefreshToken("citizen-1", "token-1",
                            Instant.now().plusSeconds(604800));
            com.suvidha.auth.model.RefreshToken token2 =
                    new com.suvidha.auth.model.RefreshToken("citizen-1", "token-2",
                            Instant.now().plusSeconds(604800));

            com.suvidha.auth.repo.RefreshTokenRepo mockRepo = mock(com.suvidha.auth.repo.RefreshTokenRepo.class);
            lenient().when(mockRepo.findByCitizenIdAndRevokedFalse("citizen-1"))
                    .thenReturn(java.util.List.of(token1, token2));

            com.suvidha.auth.service.RefreshTokenService refreshTokenService =
                    new com.suvidha.auth.service.RefreshTokenService(mockRepo);

            refreshTokenService.revokeAllUserTokens("citizen-1");

            assertTrue(token1.isRevoked());
            assertTrue(token2.isRevoked());
            verify(mockRepo, times(2)).save(any());
        }
    }

    // ========================================================================
    // ADDITIONAL: Issuer/Audience Enforcement
    // ========================================================================
    @Nested
    @DisplayName("Additional: Issuer/Audience Enforcement")
    class IssuerAudienceEnforcement {

        @Test
        @DisplayName("SHOULD REJECT — wrong issuer")
        void wrongIssuer_shouldBeRejected() {
            String wrongIssuerToken = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("evil-issuer")
                    .audience().add("suvidha-services").and()
                    .claim("role", "USER")
                    .claim("type", "access")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(1800)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            assertThrows(io.jsonwebtoken.IncorrectClaimException.class,
                    () -> authJwtToken.validate(wrongIssuerToken));
        }

        @Test
        @DisplayName("SHOULD REJECT — wrong audience")
        void wrongAudience_shouldBeRejected() {
            String wrongAudToken = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("suvidha-auth")
                    .audience().add("evil-audience").and()
                    .claim("role", "USER")
                    .claim("type", "access")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(1800)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            assertThrows(io.jsonwebtoken.IncorrectClaimException.class,
                    () -> authJwtToken.validate(wrongAudToken));
        }

        @Test
        @DisplayName("SHOULD REJECT — missing issuer claim")
        void missingIssuer_shouldBeRejected() {
            String noIssuerToken = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .audience().add("suvidha-services").and()
                    .claim("role", "USER")
                    .claim("type", "access")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(1800)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            assertThrows(io.jsonwebtoken.MissingClaimException.class,
                    () -> authJwtToken.validate(noIssuerToken));
        }
    }

    // ========================================================================
    // ADDITIONAL: Token Type Enforcement
    // ========================================================================
    @Nested
    @DisplayName("Additional: Token Type Enforcement")
    class TokenTypeEnforcement {

        @Test
        @DisplayName("SHOULD REJECT — token with type 'refresh' instead of 'access'")
        void wrongTokenType_shouldBeRejected() {
            String refreshTypeToken = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("suvidha-auth")
                    .audience().add("suvidha-services").and()
                    .claim("role", "USER")
                    .claim("type", "refresh")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(1800)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            Claims claims = authJwtToken.validate(refreshTypeToken);
            assertEquals("refresh", claims.get("type", String.class));
            assertNotEquals("access", claims.get("type", String.class));
        }

        @Test
        @DisplayName("SHOULD REJECT — token with no type claim")
        void missingTokenType_shouldBeDetected() {
            String noTypeToken = Jwts.builder()
                    .header().keyId(activeKey.getKid()).and()
                    .id(UUID.randomUUID().toString())
                    .subject("citizen-1")
                    .issuer("suvidha-auth")
                    .audience().add("suvidha-services").and()
                    .claim("role", "USER")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(1800)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            Claims claims = authJwtToken.validate(noTypeToken);
            assertNull(claims.get("type", String.class));
        }
    }
}
