package com.suvidha.gateway.jwt;

import io.jsonwebtoken.Claims;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Runs at HIGHEST_PRECEDENCE so all downstream filters see enriched headers.
 *
 * <p>Per the spec, the following headers are stripped from the incoming request
 * (to prevent spoofing) and re-added from validated JWT claims:
 * <ul>
 *   <li>{@code X-User-Id}       — citizenId claim (fallback: sub)</li>
 *   <li>{@code X-Citizen-Id}    — alias kept for backward-compatibility</li>
 *   <li>{@code X-Citizen-Name}  — name claim</li>
 *   <li>{@code X-Citizen-Mobile}— mobile claim</li>
 *   <li>{@code X-User-Role}     — role claim</li>
 *   <li>{@code X-User-Language} — language claim (fallback: lang)</li>
 *   <li>{@code X-Forwarded-For} — real client IP</li>
 * </ul>
 *
 * <p>JWT validation failures are logged via SLF4J and counted as
 * {@code gateway.jwt.validation.failures}.
 */
@Component
public class JwtClaimsToHeadersFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtClaimsToHeadersFilter.class);

    // ── header name constants ──────────────────────────────────────────────
    public static final String HEADER_USER_ID        = "X-User-Id";        // spec
    public static final String HEADER_CITIZEN_ID     = "X-Citizen-Id";     // back-compat
    public static final String HEADER_CITIZEN_NAME   = "X-Citizen-Name";
    public static final String HEADER_CITIZEN_MOBILE = "X-Citizen-Mobile";
    public static final String HEADER_USER_ROLE      = "X-User-Role";
    public static final String HEADER_USER_LANGUAGE  = "X-User-Language";

    private final JwtToken jwtToken;
    private final Counter  jwtFailureCounter;

    public JwtClaimsToHeadersFilter(JwtToken jwtToken, MeterRegistry meterRegistry) {
        this.jwtToken = jwtToken;
        this.jwtFailureCounter = Counter.builder("gateway.jwt.validation.failures")
                .description("Number of JWT validation failures at the gateway")
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Always strip spoofable headers from the incoming request first
        ServerHttpRequest.Builder reqBuilder = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(HEADER_USER_ID);
                    h.remove(HEADER_CITIZEN_ID);
                    h.remove(HEADER_CITIZEN_NAME);
                    h.remove(HEADER_CITIZEN_MOBILE);
                    h.remove(HEADER_USER_ROLE);
                    h.remove(HEADER_USER_LANGUAGE);
                });

        // Add X-Forwarded-For with real client IP
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            String clientIp = remoteAddress.getAddress().getHostAddress();
            reqBuilder.header("X-Forwarded-For", clientIp);
        }

        // If no Bearer token, forward with stripped headers only
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange.mutate().request(reqBuilder.build()).build());
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return chain.filter(exchange.mutate().request(reqBuilder.build()).build());
        }

        Claims claims;
        try {
            claims = jwtToken.validate(token);
        } catch (Exception e) {
            jwtFailureCounter.increment();
            log.warn("JWT validation failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            // Let Spring Security's AuthenticationWebFilter reject the request; don't short-circuit here
            return chain.filter(exchange.mutate().request(reqBuilder.build()).build());
        }

        return jwtToken.isBlacklisted(token)
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        jwtFailureCounter.increment();
                        log.warn("JWT is blacklisted (logout): jti={}", claims.getId());
                        return exchange.getResponse().setComplete()
                                .then(Mono.error(new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Token revoked")));
                    }

                    // Extract claims — support both claim name variants for resilience
                    String citizenId = extractString(claims, "citizenId", claims.getSubject());
                    String name      = extractString(claims, "name",      null);
                    String mobile    = extractString(claims, "mobile",    null);
                    String role      = extractString(claims, "role",      null);
                    // spec uses "language"; fall back to "lang" for backward compat
                    String language  = extractString(claims, "language",  extractString(claims, "lang", null));

                    reqBuilder.headers(h -> {
                        if (notBlank(citizenId)) {
                            h.add(HEADER_USER_ID,    citizenId);   // spec header
                            h.add(HEADER_CITIZEN_ID, citizenId);   // back-compat
                        }
                        if (notBlank(name))     h.add(HEADER_CITIZEN_NAME,   name);
                        if (notBlank(mobile))   h.add(HEADER_CITIZEN_MOBILE, mobile);
                        if (notBlank(role))     h.add(HEADER_USER_ROLE,      role);
                        if (notBlank(language)) h.add(HEADER_USER_LANGUAGE,  language);
                    });

                    return chain.filter(exchange.mutate().request(reqBuilder.build()).build());
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static String extractString(Claims claims, String key, String fallback) {
        Object val = claims.get(key);
        return (val != null && !val.toString().isBlank()) ? val.toString() : fallback;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
