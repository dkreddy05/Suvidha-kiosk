package com.suvidha.billing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("Processing request: {} {} Authorization={}",
                request.getMethod(), request.getRequestURI(),
                request.getHeader(HttpHeaders.AUTHORIZATION) != null ? "present" : "null");

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    Claims claims = jwtUtil.parseClaims(token);

                    String jti = claims.getId();
                    if (jti != null && !jti.isBlank()) {
                        Boolean isBlacklisted;
                        try {
                            isBlacklisted = redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti);
                        } catch (Exception e) {
                            log.error("Redis blacklist check failed — denying request (fail-closed): {}", e.getMessage());
                            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token validation failed");
                            return;
                        }
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            log.warn("JWT is blacklisted (logout): jti={}", jti);
                            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token revoked");
                            return;
                        }
                    }

                    String citizenId = jwtUtil.extractCitizenId(claims);
                    String mobile = jwtUtil.extractMobile(claims);
                    String role = jwtUtil.extractRole(claims);

                    if (citizenId != null && !citizenId.isBlank()) {
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        if (role != null && !role.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        }

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                citizenId, null, authorities);
                        auth.setDetails(new CitizenAuthDetails(mobile));
                        request.setAttribute("citizenMobile", mobile);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (ExpiredJwtException ex) {
                    log.warn("JWT expired for request {} {}: {}",
                            request.getMethod(), request.getRequestURI(), ex.getMessage());
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
                    return;
                } catch (Exception ex) {
                    log.warn("JWT parse failed for request {} {}: {}",
                            request.getMethod(), request.getRequestURI(),
                            ex.getClass().getSimpleName());
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "error", "Unauthorized",
                "message", message,
                "timestamp", Instant.now().toString()));
    }
}
