package com.suvidha.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.JwsHeader;
import com.suvidha.auth.service.RsaKeyService;
import com.suvidha.auth.model.JwtKeyVersion;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class JwtToken {

  private static final Logger log = LoggerFactory.getLogger(JwtToken.class);

  private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

  private final long expMillis;
  private final String issuer;
  private final String audience;
  private final RsaKeyService rsaKeyService;
  private final StringRedisTemplate redisTemplate;

  public JwtToken(
      @Value("${jwt.expMillis:1800000}") long expMillis,
      @Value("${jwt.issuer:suvidha-auth}") String issuer,
      @Value("${jwt.audience:suvidha-services}") String audience,
      RsaKeyService rsaKeyService,
      StringRedisTemplate redisTemplate) {

    this.expMillis = expMillis;
    this.issuer = issuer;
    this.audience = audience;
    this.rsaKeyService = rsaKeyService;
    this.redisTemplate = redisTemplate;
  }

  public String generateToken(String citizenId, String mobile, String name, String role) {

    JwtKeyVersion activeKey = rsaKeyService.getActiveKeyVersion();
    PrivateKey privateKey = rsaKeyService.getPrivateKey(activeKey);

    Instant now = Instant.now();
    String jti = UUID.randomUUID().toString();

    return Jwts.builder()
        .header().keyId(activeKey.getKid()).and()
        .id(jti)
        .subject(citizenId)
        .issuer(issuer)
        .audience().add(audience).and()
        .claim("mobile", mobile)
        .claim("name", name)
        .claim("role", role != null ? role : "USER")
        .claim("type", "access")
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(expMillis)))
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  // ===============================
  // 🔍 VALIDATE TOKEN
  // ===============================
  public Claims validate(String token) {

    Locator<Key> keyLocator = header -> {

      if (!(header instanceof JwsHeader jwsHeader)) {
        throw new IllegalArgumentException("Invalid JWT header");
      }

      // 🔒 Enforce RS256 ONLY (prevents algorithm attack)
      if (!"RS256".equals(jwsHeader.getAlgorithm())) {
        throw new IllegalArgumentException("Unsupported algorithm");
      }

      String kid = jwsHeader.getKeyId();
      if (kid == null) {
        throw new IllegalArgumentException("Missing key id");
      }

      PublicKey publicKey = rsaKeyService.getPublicKeyByKid(kid);

      if (publicKey == null) {
        throw new IllegalArgumentException("Unknown key id: " + kid);
      }

      return publicKey;
    };

    return Jwts.parser()
        .requireIssuer(issuer)
        .requireAudience(audience)
        .keyLocator(keyLocator)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  // ===============================
  // 📱 GET CLAIMS
  // ===============================
  public String getCitizenId(String token) {
    return validate(token).getSubject();
  }

  public String getMobile(String token) {
    return validate(token).get("mobile", String.class);
  }

  public String getName(String token) {
    return validate(token).get("name", String.class);
  }

  public String getRole(String token) {
    return validate(token).get("role", String.class);
  }

  // ===============================
  // ✅ TOKEN VALIDITY CHECK
  // ===============================
  public boolean isTokenValid(String token) {
    try {
      validate(token);
      return true;
    } catch (Exception e) {
      log.warn("JWT validation failed: {}", e.getMessage());
      return false;
    }
  }

  // ===============================
  // 🚫 TOKEN BLACKLIST (logout)
  // ===============================
  public void blacklistToken(String token) {
    try {
      Claims claims = validate(token);
      String jti = claims.getId();
      if (jti == null || jti.isBlank()) {
        log.warn("Cannot blacklist token: missing jti claim");
        return;
      }
      Instant expiration = claims.getExpiration().toInstant();
      long ttlSeconds = Math.max(1, expiration.getEpochSecond() - Instant.now().getEpochSecond());
      String key = BLACKLIST_KEY_PREFIX + jti;
      redisTemplate.opsForValue().set(key, "1", java.time.Duration.ofSeconds(ttlSeconds));
      log.info("Blacklisted token jti={} with TTL={}s", jti, ttlSeconds);
    } catch (Exception e) {
      log.warn("Failed to blacklist token: {}", e.getMessage());
    }
  }

  public boolean isBlacklisted(String token) {
    try {
      Claims claims = validate(token);
      String jti = claims.getId();
      if (jti == null || jti.isBlank()) {
        return false;
      }
      Boolean exists = redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti);
      return Boolean.TRUE.equals(exists);
    } catch (Exception e) {
      return false;
    }
  }
}