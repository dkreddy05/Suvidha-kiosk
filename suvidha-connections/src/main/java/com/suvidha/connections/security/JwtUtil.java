package com.suvidha.connections.security;

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
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private static final Duration KEY_TTL = Duration.ofHours(1);

    private final Map<String, KeyCacheEntry> rsaKeys = new ConcurrentHashMap<>();
    private final String authServiceUrl;
    private final RestTemplate restTemplate;

    public JwtUtil(@Value("${auth.service.url:http://suvidha-auth:8081}") String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
        this.restTemplate = new RestTemplate();
    }

    public Claims parseClaims(String token) {
        Locator<Key> keyLocator = header -> {
            if (!(header instanceof JwsHeader jwsHeader)) {
                throw new IllegalArgumentException("Invalid JWT header");
            }
            String alg = jwsHeader.getAlgorithm();
            if (!"RS256".equals(alg)) {
                throw new IllegalArgumentException("Unsupported JWT algorithm: " + alg + ". Only RS256 is accepted.");
            }
            String kid = jwsHeader.getKeyId();
            if (kid == null) {
                if (rsaKeys.isEmpty()) {
                    throw new IllegalStateException("No RSA public keys available");
                }
                return rsaKeys.values().iterator().next().key();
            }
            KeyCacheEntry entry = rsaKeys.get(kid);
            if (entry == null) {
                fetchKeys();
                entry = rsaKeys.get(kid);
            }
            if (entry == null || isStale(entry)) {
                fetchKeys();
                entry = rsaKeys.get(kid);
            }
            if (entry == null) {
                throw new IllegalArgumentException("Unknown key id: " + kid);
            }
            return entry.key();
        };

        return Jwts.parser()
                .keyLocator(keyLocator)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void fetchKeys() {
        try {
            String url = authServiceUrl + "/api/auth/public-key";
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                for (Object keyObj : response.getBody()) {
                    Map<String, Object> keyData = (Map<String, Object>) keyObj;
                    String kid = (String) keyData.getOrDefault("kid", "default");
                    String pubKeyStr = keyData.containsKey("public_key")
                            ? (String) keyData.get("public_key")
                            : (String) keyData.get("publicKey");
                    if (pubKeyStr != null) {
                        pubKeyStr = pubKeyStr
                                .replace("-----BEGIN PUBLIC KEY-----", "")
                                .replace("-----END PUBLIC KEY-----", "")
                                .replaceAll("\\s", "");
                        byte[] decoded = Base64.getDecoder().decode(pubKeyStr);
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        Key rsaKey = keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
                        rsaKeys.put(kid, new KeyCacheEntry(rsaKey, Instant.now()));
                        log.debug("Cached RSA key: {}", kid);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch RSA public keys from auth service: {}", e.getMessage());
        }
    }

    private boolean isStale(KeyCacheEntry entry) {
        return entry != null && Instant.now().isAfter(entry.fetchedAt().plus(KEY_TTL));
    }

    public String extractCitizenId(Claims claims) {
        Object citizenId = claims.get("citizenId");
        if (citizenId != null && !citizenId.toString().isBlank()) {
            return citizenId.toString();
        }
        return claims.getSubject();
    }

    public String extractMobile(Claims claims) {
        Object mobile = claims.get("mobile");
        return mobile != null ? mobile.toString() : null;
    }

    private record KeyCacheEntry(Key key, Instant fetchedAt) {}
}
