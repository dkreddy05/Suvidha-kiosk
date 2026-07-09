package com.suvidha.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);
    private static final Duration KEY_TTL = Duration.ofHours(1);

    private final String publicKeyUrl;
    private final String jwtIssuer;
    private final String jwtAudience;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, KeyCacheEntry> rsaKeys = new ConcurrentHashMap<>();
    private volatile Instant lastFetchAttempt = Instant.EPOCH;

    public JwtTokenValidator(String publicKeyUrl, String jwtIssuer, String jwtAudience) {
        this.publicKeyUrl = publicKeyUrl;
        this.jwtIssuer = jwtIssuer;
        this.jwtAudience = jwtAudience;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public Claims parseClaims(String token) {
        refreshKeysIfStale();
        return Jwts.parser()
                .keyLocator((Locator<Key>) header -> resolveKey((JwsHeader) header))
                .requireIssuer(jwtIssuer)
                .requireAudience(jwtAudience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key resolveKey(JwsHeader header) {
        String alg = header.getAlgorithm();
        if (!"RS256".equals(alg)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + alg + ". Only RS256 is accepted.");
        }
        String kid = header.getKeyId();
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
    }

    private void refreshKeysIfStale() {
        Instant now = Instant.now();
        boolean anyStale = rsaKeys.values().stream()
                .anyMatch(e -> now.isAfter(e.fetchedAt().plus(KEY_TTL)));
        if (!anyStale && !rsaKeys.isEmpty()) return;
        if (lastFetchAttempt.plusSeconds(30).isAfter(now)) return;
        lastFetchAttempt = now;

        try {
            String response = restTemplate.getForObject(publicKeyUrl, String.class);
            if (response == null) return;

            List<Map<String, Object>> keysList = objectMapper.readValue(
                    response, new TypeReference<List<Map<String, Object>>>() {});

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
                byte[] decoded = Base64.getDecoder().decode(pubKeyStr);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                Key key = kf.generatePublic(new X509EncodedKeySpec(decoded));
                rsaKeys.put(kid, new KeyCacheEntry(key, Instant.now()));
                log.info("Cached RSA key: {}", kid);
            }
        } catch (Exception e) {
            log.error("Failed to fetch/parse public keys from {}", publicKeyUrl, e);
        }
    }

    private record KeyCacheEntry(Key key, Instant fetchedAt) {}
}
