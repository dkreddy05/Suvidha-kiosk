package com.suvidha.gateway.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Locator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class JwtToken {
    private static final Logger log = LoggerFactory.getLogger(JwtToken.class);

    private static final Duration KEY_TTL = Duration.ofHours(1);
    private static final Duration NEGATIVE_KID_TTL = Duration.ofMinutes(5);
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    // Circuit breaker settings for Redis blacklist checks
    private static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5;
    private static final Duration CIRCUIT_BREAKER_COOLDOWN = Duration.ofSeconds(30);

    private final String publicKeyUrl;
    private final String jwtIssuer;
    private final String jwtAudience;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final WebClient webClient;

    private final Map<String, KeyCacheEntry> rsaKeys = new ConcurrentHashMap<>();
    private final Map<String, Instant> negativeKidCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile Instant lastFetchAttempt = Instant.EPOCH;

    // Circuit breaker state
    private final AtomicInteger consecutiveRedisFailures = new AtomicInteger(0);
    private final AtomicReference<Instant> circuitOpenedAt = new AtomicReference<>(null);

    public JwtToken(
            @Value("${jwt.publicKeyUrl:http://localhost:8081/api/auth/public-key}") String publicKeyUrl,
            @Value("${jwt.issuer:suvidha-auth}") String jwtIssuer,
            @Value("${jwt.audience:suvidha-services}") String jwtAudience,
            ReactiveStringRedisTemplate redisTemplate,
            WebClient.Builder webClientBuilder) {
        this.publicKeyUrl = publicKeyUrl;
        this.jwtIssuer = jwtIssuer;
        this.jwtAudience = jwtAudience;
        this.redisTemplate = redisTemplate;
        this.webClient = webClientBuilder.build();
    }

    private Mono<Void> refreshKeysAsync() {
        Instant now = Instant.now();
        negativeKidCache.entrySet().removeIf(entry -> now.isAfter(entry.getValue().plus(NEGATIVE_KID_TTL)));

        boolean anyStale = rsaKeys.values().stream()
                .anyMatch(e -> now.isAfter(e.fetchedAt().plus(KEY_TTL)));
        if (!anyStale && !rsaKeys.isEmpty()) return Mono.empty();
        if (lastFetchAttempt.plusSeconds(30).isAfter(now)) return Mono.empty();
        lastFetchAttempt = now;

        log.info("Fetching/refreshing RSA public keys from auth service");
        return webClient.get()
                .uri(publicKeyUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .onErrorResume(e -> {
                    log.error("Failed to fetch public keys: {}", e.getMessage());
                    return Mono.empty();
                })
                .flatMap(keysList -> {
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
                            // Remove from negative cache if it was previously unknown
                            negativeKidCache.remove(kid);
                            log.info("Cached RSA key: {}", kid);
                        } catch (Exception e) {
                            log.warn("Failed to parse public key {}: {}", kid, e.getMessage());
                        }
                    }
                    return Mono.<Void>empty();
                });
    }

    private Key resolveKey(JwsHeader jwsHeader) {
        String alg = jwsHeader.getAlgorithm();
        if (!"RS256".equals(alg)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + alg + ". Only RS256 is accepted.");
        }
        String kid = jwsHeader.getKeyId();
        if (kid == null) {
            if (rsaKeys.isEmpty()) {
                throw new IllegalStateException("No RSA public keys available");
            }
            return rsaKeys.values().iterator().next().key();
        }

        // Check negative cache first — prevents DDoS on auth service via crafted kid values
        Instant negativeCachedAt = negativeKidCache.get(kid);
        if (negativeCachedAt != null && Instant.now().isBefore(negativeCachedAt.plus(NEGATIVE_KID_TTL))) {
            throw new IllegalArgumentException("Unknown key id (cached negative): " + kid);
        }

        KeyCacheEntry entry = rsaKeys.get(kid);
        if (entry == null) {
            // Kid not found even after refresh — add to negative cache
            negativeKidCache.put(kid, Instant.now());
            throw new IllegalArgumentException("Unknown key id: " + kid);
        }
        return entry.key();
    }

    public Mono<Claims> validateAsync(String token) {
        return refreshKeysAsync().then(Mono.fromCallable(() ->
            Jwts.parser()
                    .keyLocator((Locator<Key>) header -> resolveKey((JwsHeader) header))
                    .requireIssuer(jwtIssuer)
                    .requireAudience(jwtAudience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
        ).subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<String> getMobileAsync(String token) {
        return validateAsync(token).map(claims -> claims.get("mobile", String.class));
    }

    public Mono<String> getCitizenIdAsync(String token) {
        return validateAsync(token).map(Claims::getSubject);
    }

    public Mono<Boolean> isTokenValidAsync(String token) {
        return validateAsync(token)
                .flatMap(claims -> isBlacklisted(claims.getId()))
                .onErrorResume(e -> {
                    log.warn("Token validation failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Checks if a JWT is blacklisted in Redis, with circuit breaker protection.
     *
     * Normal operation (circuit closed): fail-closed — Redis errors reject the request.
     * During prolonged outage (circuit open): fail-open — allows requests through with a warning,
     * re-tests Redis after a cooldown period.
     */
    public Mono<Boolean> isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return Mono.just(false);
        }

        // Circuit breaker: check if circuit is open
        Instant openedAt = circuitOpenedAt.get();
        if (openedAt != null) {
            if (Instant.now().isBefore(openedAt.plus(CIRCUIT_BREAKER_COOLDOWN))) {
                // Circuit is open — fail-open (allow request, log warning)
                log.warn("Redis circuit breaker OPEN — allowing request without blacklist check for jti={}", jti);
                return Mono.just(false);
            }
            // Cooldown elapsed — try half-open (reset and test Redis)
            log.info("Redis circuit breaker attempting half-open reset");
            circuitOpenedAt.set(null);
            consecutiveRedisFailures.set(0);
        }

        return redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti)
                .doOnNext(result -> {
                    // Successful Redis call — reset circuit breaker
                    consecutiveRedisFailures.set(0);
                    circuitOpenedAt.set(null);
                })
                .onErrorResume(e -> {
                    int failures = consecutiveRedisFailures.incrementAndGet();
                    if (failures >= CIRCUIT_BREAKER_FAILURE_THRESHOLD) {
                        // Trip the circuit
                        circuitOpenedAt.set(Instant.now());
                        log.error("Redis circuit breaker TRIPPED after {} consecutive failures — " +
                                "will fail-open for {}s: {}",
                                failures, CIRCUIT_BREAKER_COOLDOWN.getSeconds(), e.getMessage());
                        // Fail-open when circuit trips: allow the request
                        return Mono.just(false);
                    }
                    // Below threshold: maintain fail-closed behavior (security > availability)
                    log.error("Redis blacklist check failed ({}/{} before circuit trip) — " +
                            "denying request (fail-closed): {}",
                            failures, CIRCUIT_BREAKER_FAILURE_THRESHOLD, e.getMessage());
                    return Mono.just(true);
                });
    }

    private record KeyCacheEntry(Key key, Instant fetchedAt) {}
}
