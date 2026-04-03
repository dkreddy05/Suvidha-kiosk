package com.suvidha.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtToken {
  private final SecretKey signKey;
  private final long expMillis;

  public JwtToken(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expMillis:1800000}") long expMillis) {
    this.signKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expMillis = expMillis;
  }

  public String generateToken(String mobile) {
    return Jwts.builder().subject(mobile).issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plusMillis(expMillis)))
        .signWith(signKey).compact();

  }

  public Claims validate(String token) {
    return Jwts.parser()
        .verifyWith(signKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public String getMobile(String token) {
    return getClaims(token).getSubject();
  }

  private Claims getClaims(String token) {
    return validate(token);
  }

  public boolean isTokenValid(String token) {
    try {
      validate(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
