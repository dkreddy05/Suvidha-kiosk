package com.suvidha.billing.security;

import com.suvidha.billing.config.JwtProperties;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT utility for the billing service.
 * Only accepts RS256-signed tokens validated against public keys fetched from the auth service.
 * HMAC fallback has been removed to prevent token forgery attacks.
 */
@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final JwtProperties props;
    private final Map<String, Key> rsaKeys = new ConcurrentHashMap<>();
    private final String authServiceUrl;
    private final RestTemplate restTemplate;

    public JwtUtil(JwtProperties props,
            @Value("${auth.service.url:http://suvidha-auth:8081}") String authServiceUrl) {
        this.props = props;
        this.authServiceUrl = authServiceUrl;
        this.restTemplate = new RestTemplate();
    }

    public Claims parseClaims(String token) {
        Locator<Key> keyLocator = header -> {
            if (!(header instanceof JwsHeader)) {
                throw new IllegalArgumentException("Invalid JWT header");
            }
            JwsHeader jwsHeader = (JwsHeader) header;
            String alg = jwsHeader.getAlgorithm();

            // SECURITY: Only accept RS256 tokens — no HMAC fallback
            if (!"RS256".equals(alg)) {
                throw new IllegalArgumentException(
                        "Unsupported JWT algorithm: " + alg + ". Only RS256 is accepted.");
            }

            String kid = jwsHeader.getKeyId();
            if (kid == null) {
                throw new IllegalArgumentException("Missing key id (kid) in JWT header");
            }

            fetchKeysIfMissing(kid);

            Key rsaKey = rsaKeys.get(kid);
            if (rsaKey == null) {
                throw new IllegalArgumentException(
                        "Unknown RSA key id: " + kid + ". Token cannot be validated.");
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
        try {
            String url = authServiceUrl + "/api/auth/public-key";
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<?> keysList = (List<?>) response.getBody();
                for (Object keyObj : keysList) {
                    Map<String, Object> keyData = (Map<String, Object>) keyObj;
                    String kId = (String) keyData.get("kid");
                    String pubKeyStr = (String) keyData.get("public_key");
                    if (pubKeyStr != null && kId != null) {
                        pubKeyStr = pubKeyStr.replace("-----BEGIN PUBLIC KEY-----", "")
                                .replace("-----END PUBLIC KEY-----", "")
                                .replaceAll("\\s", "");
                        byte[] decoded = Base64.getDecoder().decode(pubKeyStr);
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        Key rsaKey = keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
                        rsaKeys.put(kId, rsaKey);
                        log.debug("Cached RSA key: {}", kId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch RSA public keys from auth service: {}", e.getMessage());
        }
    }

    public String extractCitizenId(Claims claims) {
        Object v = claims.get(props.getCitizenIdClaim());
        if (v != null && !v.toString().isBlank()) {
            return v.toString();
        }
        return claims.getSubject();
    }

    public String extractMobile(Claims claims) {
        Object v = claims.get(props.getMobileClaim());
        return v == null ? null : v.toString();
    }

    public String extractRole(Claims claims) {
        Object v = claims.get(props.getRoleClaim());
        return v == null ? null : v.toString();
    }
}