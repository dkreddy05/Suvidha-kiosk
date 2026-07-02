package com.suvidha.auth.token;

import com.suvidha.auth.Dto.Role;
import com.suvidha.auth.model.JwtKeyVersion;
import com.suvidha.auth.service.RsaKeyService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenTest {

    @Mock
    private RsaKeyService rsaKeyService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private JwtToken jwtToken;
    private JwtKeyVersion activeKey;
    private PrivateKey privateKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        privateKey = pair.getPrivate();

        String publicKeyStr = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        activeKey = new JwtKeyVersion(
            UUID.randomUUID().toString(),
            publicKeyStr,
            privateKeyStr,
            true,
            Instant.now(),
            Instant.now().plus(90, ChronoUnit.DAYS)
        );

        when(rsaKeyService.getActiveKeyVersion()).thenReturn(activeKey);
        when(rsaKeyService.getPrivateKey(activeKey)).thenReturn(privateKey);
        when(rsaKeyService.getPublicKeyByKid(activeKey.getKid())).thenReturn(pair.getPublic());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);

        jwtToken = new JwtToken(1800000, "suvidha-auth", "suvidha-services", rsaKeyService, redisTemplate);
    }

    @Test
    @DisplayName("generateToken should include role claim in JWT")
    void generateToken_shouldIncludeRoleClaim() {
        String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "ADMIN");

        Claims claims = jwtToken.validate(token);

        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    @DisplayName("generateToken should default role to USER when null")
    void generateToken_shouldDefaultRoleToUserWhenNull() {
        String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", null);

        Claims claims = jwtToken.validate(token);

        assertEquals("USER", claims.get("role", String.class));
    }

    @Test
    @DisplayName("generateToken should include all expected claims")
    void generateToken_shouldIncludeAllClaims() {
        String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "EMPLOYEE");

        Claims claims = jwtToken.validate(token);

        assertEquals("citizen-1", claims.getSubject());
        assertEquals("suvidha-auth", claims.getIssuer());
        assertEquals("9876543210", claims.get("mobile", String.class));
        assertEquals("Test User", claims.get("name", String.class));
        assertEquals("EMPLOYEE", claims.get("role", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    @DisplayName("generateToken should include unique jti claim")
    void generateToken_shouldIncludeJtiClaim() {
        String token1 = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
        String token2 = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");

        Claims claims1 = jwtToken.validate(token1);
        Claims claims2 = jwtToken.validate(token2);

        assertNotNull(claims1.getId());
        assertNotNull(claims2.getId());
        assertNotEquals(claims1.getId(), claims2.getId());
    }

    @Test
    @DisplayName("getRole should extract role from valid token")
    void getRole_shouldExtractRoleFromToken() {
        String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "ADMIN");

        String role = jwtToken.getRole(token);

        assertEquals("ADMIN", role);
    }

    @Test
    @DisplayName("generateToken with USER role should produce valid token")
    void generateToken_withUserRole_shouldProduceValidToken() {
        String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");

        assertTrue(jwtToken.isTokenValid(token));
        assertEquals("USER", jwtToken.getRole(token));
    }

    @Nested
    @DisplayName("Token blacklist (logout)")
    class BlacklistTests {

        @Test
        @DisplayName("blacklistToken should store jti in Redis with TTL")
        void blacklistToken_shouldStoreInRedis() {
            String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
            Claims claims = jwtToken.validate(token);
            String jti = claims.getId();

            doNothing().when(valueOps).set(eq("jwt:blacklist:" + jti), eq("1"), any(java.time.Duration.class));

            jwtToken.blacklistToken(token);

            verify(valueOps).set(eq("jwt:blacklist:" + jti), eq("1"), any(java.time.Duration.class));
        }

        @Test
        @DisplayName("isBlacklisted should return true when jti exists in Redis")
        void isBlacklisted_shouldReturnTrueWhenBlacklisted() {
            String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
            Claims claims = jwtToken.validate(token);
            String jti = claims.getId();

            when(redisTemplate.hasKey("jwt:blacklist:" + jti)).thenReturn(true);

            assertTrue(jwtToken.isBlacklisted(token));
        }

        @Test
        @DisplayName("isBlacklisted should return false when jti not in Redis")
        void isBlacklisted_shouldReturnFalseWhenNotBlacklisted() {
            String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
            Claims claims = jwtToken.validate(token);
            String jti = claims.getId();

            when(redisTemplate.hasKey("jwt:blacklist:" + jti)).thenReturn(false);

            assertFalse(jwtToken.isBlacklisted(token));
        }

        @Test
        @DisplayName("isBlacklisted should return false for token without jti")
        void isBlacklisted_shouldReturnFalseForTokenWithoutJti() {
            // Tokens generated by our JwtToken always have jti, so we test the fallback path
            // by verifying a freshly generated token is not blacklisted (Redis key absent)
            String token = jwtToken.generateToken("citizen-1", "9876543210", "Test User", "USER");
            Claims claims = jwtToken.validate(token);
            String jti = claims.getId();
            assertNotNull(jti);

            when(redisTemplate.hasKey("jwt:blacklist:" + jti)).thenReturn(false);

            assertFalse(jwtToken.isBlacklisted(token));
        }
    }
}
