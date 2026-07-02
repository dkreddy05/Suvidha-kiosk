package com.suvidha.grievance.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.JwsHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private static final Duration KEY_TTL = Duration.ofHours(1);

    private final String authServiceUrl;
    private final Map<String, KeyCacheEntry> rsaKeys = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile Instant lastFetchAttempt = Instant.EPOCH;

    public JwtUtil(
            @Value("${auth.service.url:http://suvidha-auth:8081}") String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
    }

    private void refreshKeys() {
        Instant now = Instant.now();
        boolean anyStale = rsaKeys.values().stream()
                .anyMatch(e -> now.isAfter(e.fetchedAt().plus(KEY_TTL)));
        if (!anyStale && !rsaKeys.isEmpty()) return;

        if (lastFetchAttempt.plusSeconds(30).isAfter(now)) return;
        lastFetchAttempt = now;

        log.info("Fetching RSA public keys from auth service");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authServiceUrl + "/api/auth/public-key"))
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
                    if (pubKeyStr != null) {
                        pubKeyStr = pubKeyStr
                                .replace("-----BEGIN PUBLIC KEY-----", "")
                                .replace("-----END PUBLIC KEY-----", "")
                                .replaceAll("\\s", "");
                        byte[] decoded = Base64.getDecoder().decode(pubKeyStr);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        Key key = kf.generatePublic(new X509EncodedKeySpec(decoded));
                        rsaKeys.put(kid, new KeyCacheEntry(key, Instant.now()));
                        log.debug("Cached RSA key: {}", kid);
                    }
                }
            } else {
                log.warn("Auth service returned HTTP {} when fetching public keys", response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to fetch public keys from auth service", e);
        }
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
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractCitizenId(Claims claims) {
        Object citizenId = claims.get("citizenId");
        if (citizenId != null && !citizenId.toString().isBlank()) {
            return citizenId.toString();
        }
        return claims.getSubject();
    }

    private record KeyCacheEntry(Key key, Instant fetchedAt) {}
}
