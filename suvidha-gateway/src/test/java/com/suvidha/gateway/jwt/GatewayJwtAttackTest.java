package com.suvidha.gateway.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Gateway-specific JWT attack tests.
 * Covers: blacklist fail-closed behavior under Redis failure.
 */
@ExtendWith(MockitoExtension.class)
class GatewayJwtAttackTest {

    @Mock
    private ReactiveStringRedisTemplate reactiveRedisTemplate;

    private JwtToken gatewayJwtToken;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String kid;
    private String validToken;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKey = pair.getPublic();
        kid = UUID.randomUUID().toString();

        gatewayJwtToken = new JwtToken(
                "http://localhost:8081/api/auth/public-key",
                "suvidha-auth",
                "suvidha-services",
                reactiveRedisTemplate
        );

        injectPublicKey(gatewayJwtToken, kid, publicKey);

        validToken = Jwts.builder()
                .header().keyId(kid).and()
                .id(UUID.randomUUID().toString())
                .subject("citizen-1")
                .issuer("suvidha-auth")
                .audience().add("suvidha-services").and()
                .claim("mobile", "9876543210")
                .claim("name", "Test User")
                .claim("role", "USER")
                .claim("type", "access")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(1800)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private void injectPublicKey(JwtToken token, String kid, PublicKey key) throws Exception {
        Field field = JwtToken.class.getDeclaredField("rsaKeys");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Object> rsaKeys = (ConcurrentHashMap<String, Object>) field.get(token);
        Class<?> entryClass = Class.forName("com.suvidha.gateway.jwt.JwtToken$KeyCacheEntry");
        var constructor = entryClass.getDeclaredConstructor(Key.class, Instant.class);
        constructor.setAccessible(true);
        Object entry = constructor.newInstance(key, Instant.now());
        rsaKeys.put(kid, entry);
    }

    @Nested
    @DisplayName("Gateway Blacklist Fail-Closed")
    class GatewayBlacklistFailClosed {

        @Test
        @DisplayName("FIXED — gateway fails closed when Redis is down (tokens treated as blacklisted)")
        void redisDown_gatewayFailsClosed() {
            when(reactiveRedisTemplate.hasKey(anyString()))
                    .thenReturn(Mono.error(new RuntimeException("Redis connection refused")));

            Mono<Boolean> result = gatewayJwtToken.isBlacklisted(validToken);
            assertTrue(result.block());
        }

        @Test
        @DisplayName("FIXED — gateway fails closed on Redis timeout")
        void redisTimeout_gatewayFailsClosed() {
            when(reactiveRedisTemplate.hasKey(anyString()))
                    .thenReturn(Mono.error(new java.util.concurrent.TimeoutException("Redis timeout")));

            Mono<Boolean> result = gatewayJwtToken.isBlacklisted(validToken);
            assertTrue(result.block());
        }

        @Test
        @DisplayName("SHOULD BLOCK — gateway correctly blocks when Redis is healthy")
        void redisHealthy_blacklistedTokenBlocked() {
            when(reactiveRedisTemplate.hasKey(anyString())).thenReturn(Mono.just(true));

            Mono<Boolean> result = gatewayJwtToken.isBlacklisted(validToken);
            assertTrue(result.block());
        }

        @Test
        @DisplayName("SHOULD ALLOW — gateway allows when Redis is healthy and token is not blacklisted")
        void redisHealthy_tokenNotBlacklisted() {
            when(reactiveRedisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

            Mono<Boolean> result = gatewayJwtToken.isBlacklisted(validToken);
            assertFalse(result.block());
        }
    }
}
