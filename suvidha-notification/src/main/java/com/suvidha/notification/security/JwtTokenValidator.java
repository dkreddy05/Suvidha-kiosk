package com.suvidha.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.JwsHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private static final Duration NEGATIVE_KID_TTL = Duration.ofMinutes(5);
    private static final Duration MIN_FETCH_INTERVAL = Duration.ofSeconds(30);

    private final Map<String, Key> rsaKeys = new ConcurrentHashMap<>();
    private final Map<String, Instant> negativeKidCache = new ConcurrentHashMap<>();
    private final String authServiceUrl;
    private final RestTemplate restTemplate;
    private volatile Instant lastFetchAttempt = Instant.EPOCH;

    public JwtTokenValidator(
            @Value("${auth.service.url:http://suvidha-auth:8081}") String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
        this.restTemplate = new RestTemplate();
    }

    public Claims validate(String token) {
        Locator<Key> keyLocator = header -> {
            if (!(header instanceof JwsHeader jwsHeader)) {
                throw new IllegalArgumentException("Invalid JWT header");
            }
            String alg = jwsHeader.getAlgorithm();
            if (!"RS256".equals(alg)) {
                throw new IllegalArgumentException("Unsupported JWT algorithm: " + alg);
            }
            String kid = jwsHeader.getKeyId();
            if (kid == null) {
                throw new IllegalArgumentException("Missing key id (kid) in JWT header");
            }

            // Check negative cache — prevents DDoS via crafted kid values
            Instant negativeCachedAt = negativeKidCache.get(kid);
            if (negativeCachedAt != null && Instant.now().isBefore(negativeCachedAt.plus(NEGATIVE_KID_TTL))) {
                throw new IllegalArgumentException("Unknown key id (cached negative): " + kid);
            }

            fetchKeysIfMissing(kid);
            Key rsaKey = rsaKeys.get(kid);
            if (rsaKey == null) {
                negativeKidCache.put(kid, Instant.now());
                throw new IllegalArgumentException("Unknown RSA key id: " + kid);
            }
            return rsaKey;
        };

        return Jwts.parser()
                .keyLocator(keyLocator)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void fetchKeysIfMissing(String kid) {
        if (rsaKeys.containsKey(kid)) return;

        // Rate limit: don't fetch more than once every 30 seconds
        Instant now = Instant.now();
        if (lastFetchAttempt.plus(MIN_FETCH_INTERVAL).isAfter(now)) return;
        lastFetchAttempt = now;

        try {
            String url = authServiceUrl + "/api/auth/public-key";
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<?> keysList;
                if (response.getBody() instanceof List) {
                    keysList = (List<?>) response.getBody();
                } else if (response.getBody() instanceof Map) {
                    keysList = List.of((Map<String, Object>) response.getBody());
                } else {
                    throw new IllegalStateException("Unexpected response type from auth service");
                }
                for (Object keyObj : keysList) {
                    Map<String, Object> keyData = (Map<String, Object>) keyObj;
                    String kId = keyData.containsKey("kid") ? (String) keyData.get("kid") : (String) keyData.get("keyId");
                    String pubKeyStr = keyData.containsKey("public_key") ? (String) keyData.get("public_key") : (String) keyData.get("publicKey");
                    if (pubKeyStr != null && kId != null) {
                        pubKeyStr = pubKeyStr.replace("-----BEGIN PUBLIC KEY-----", "")
                                .replace("-----END PUBLIC KEY-----", "")
                                .replaceAll("\\s", "");
                        byte[] decoded = Base64.getDecoder().decode(pubKeyStr);
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        Key rsaKey = keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
                        rsaKeys.put(kId, rsaKey);
                        negativeKidCache.remove(kId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch RSA public keys from auth service: {}", e.getMessage());
        }
    }
}
