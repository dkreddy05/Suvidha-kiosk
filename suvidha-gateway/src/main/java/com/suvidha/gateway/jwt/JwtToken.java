package com.suvidha.gateway.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.JwsHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtToken {
    private static final Logger log = LoggerFactory.getLogger(JwtToken.class);

    private static final Duration KEY_TTL = Duration.ofHours(1);
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final String publicKeyUrl;
    private final String jwtIssuer;
    private final String jwtAudience;
    private final ReactiveStringRedisTemplate redisTemplate;

    private final Map<String, KeyCacheEntry> rsaKeys = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile Instant lastFetchAttempt = Instant.EPOCH;

    public JwtToken(
            @Value("${jwt.publicKeyUrl:http://localhost:8081/api/auth/public-key}") String publicKeyUrl,
            @Value("${jwt.issuer:suvidha-auth}") String jwtIssuer,
            @Value("${jwt.audience:suvidha-services}") String jwtAudience,
            ReactiveStringRedisTemplate redisTemplate) {
        this.publicKeyUrl = publicKeyUrl;
        this.jwtIssuer = jwtIssuer;
        this.jwtAudience = jwtAudience;
        this.redisTemplate = redisTemplate;
    }

    private void refreshKeys() {
        Instant now = Instant.now();
        boolean anyStale = rsaKeys.values().stream()
                .anyMatch(e -> now.isAfter(e.fetchedAt().plus(KEY_TTL)));
        if (!anyStale && !rsaKeys.isEmpty()) return;

        if (lastFetchAttempt.plusSeconds(30).isAfter(now)) return;
        lastFetchAttempt = now;

        log.info("Fetching/refreshing RSA public keys from auth service");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(publicKeyUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<Map<String, Object>> keysList = objectMapper.readValue(
                        response.body(), new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> keyData : keysList) {
                    String kid = (String) keyData.getOrDefault("kid", "default");
                    String pubKeyStr = keyData.containsKey("public_key")
                            ? (String) keyData.get("public_key")
                            : (String) keyData.get("publicKey");
                    if (pubKeyStr == null) continue;

                    pubKeyStr = pubKeyStr
                            .replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", "")
                            .replaceAll("\\s", "");
                    try {
                        byte[] decoded = Base64.getDecoder().decode(pubKeyStr);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        Key key = kf.generatePublic(new X509EncodedKeySpec(decoded));
                        rsaKeys.put(kid, new KeyCacheEntry(key, Instant.now()));
                        log.info("Cached RSA key: {}", kid);
                    } catch (Exception e) {
                        log.warn("Failed to parse public key {}: {}", kid, e.getMessage());
                    }
                }
            } else {
                log.warn("Auth service returned HTTP {} when fetching public keys", response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to fetch/parse public keys from auth service", e);
        }
    }

    public Claims validate(String token) {
        Locator<Key> keyLocator = header -> {
            if (!(header instanceof JwsHeader jwsHeader)) {
                throw new IllegalArgumentException("Invalid JWT header");
            }
            String alg = jwsHeader.getAlgorithm();
            if (!"RS256".equals(alg)) {
                throw new IllegalArgumentException("Unsupported algorithm: " + alg + ". Only RS256 is accepted.");
            }
            refreshKeys();
            String kid = jwsHeader.getKeyId();
            if (kid == null) {
                if (rsaKeys.isEmpty()) {
                    throw new IllegalStateException("No RSA public keys available");
                }
                return rsaKeys.values().iterator().next().key();
            }
            KeyCacheEntry entry = rsaKeys.get(kid);
            if (entry == null) {
                throw new IllegalArgumentException("Unknown key id: " + kid);
            }
            return entry.key();
        };

        return Jwts.parser()
                .keyLocator(keyLocator)
                .requireIssuer(jwtIssuer)
                .requireAudience(jwtAudience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getMobile(String token) {
        return validate(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            validate(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public reactor.core.publisher.Mono<Boolean> isBlacklisted(String token) {
        try {
            Claims claims = validate(token);
            String jti = claims.getId();
            if (jti == null || jti.isBlank()) {
                return reactor.core.publisher.Mono.just(false);
            }
            return redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti)
                    .onErrorResume(e -> {
                        log.error("Redis blacklist check failed — denying request (fail-closed): {}", e.getMessage());
                        return reactor.core.publisher.Mono.just(true);
                    });
        } catch (Exception e) {
            return reactor.core.publisher.Mono.just(true);
        }
    }

    private record KeyCacheEntry(Key key, Instant fetchedAt) {}
}
