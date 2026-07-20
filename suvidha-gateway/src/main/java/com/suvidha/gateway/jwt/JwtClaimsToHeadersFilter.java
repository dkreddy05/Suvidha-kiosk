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
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component
public class JwtClaimsToHeadersFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtClaimsToHeadersFilter.class);

    public static final String HEADER_USER_ID        = "X-User-Id";
    public static final String HEADER_CITIZEN_ID     = "X-Citizen-Id";
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

        ServerHttpRequest.Builder reqBuilder = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(HEADER_USER_ID);
                    h.remove(HEADER_CITIZEN_ID);
                    h.remove(HEADER_CITIZEN_NAME);
                    h.remove(HEADER_CITIZEN_MOBILE);
                    h.remove(HEADER_USER_ROLE);
                    h.remove(HEADER_USER_LANGUAGE);
                });

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            String clientIp = remoteAddress.getAddress().getHostAddress();
            reqBuilder.header("X-Forwarded-For", clientIp);
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange.mutate().request(reqBuilder.build()).build());
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return chain.filter(exchange.mutate().request(reqBuilder.build()).build());
        }

        return jwtToken.validateAsync(token)
                .flatMap(claims -> jwtToken.isBlacklisted(claims.getId())
                        .flatMap(blacklisted -> {
                            if (blacklisted) {
                                jwtFailureCounter.increment();
                                log.warn("JWT is blacklisted (logout): jti={}", claims.getId());
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().<Boolean>setComplete().then(Mono.just(false));
                            }

                            String citizenId = extractString(claims, "citizenId", claims.getSubject());
                            String name      = extractString(claims, "name",      null);
                            String mobile    = extractString(claims, "mobile",    null);
                            String role      = extractString(claims, "role",      null);
                            String language  = extractString(claims, "language",  extractString(claims, "lang", null));

                            reqBuilder.headers(h -> {
                                if (notBlank(citizenId)) {
                                    h.add(HEADER_USER_ID,    citizenId);
                                    h.add(HEADER_CITIZEN_ID, citizenId);
                                }
                                if (notBlank(name))     h.add(HEADER_CITIZEN_NAME,   name);
                                if (notBlank(mobile))   h.add(HEADER_CITIZEN_MOBILE, mobile);
                                if (notBlank(role))     h.add(HEADER_USER_ROLE,      role);
                                if (notBlank(language)) h.add(HEADER_USER_LANGUAGE,  language);
                            });

                            return Mono.just(true);
                        }))
                .onErrorResume(ResponseStatusException.class, e -> {
                    jwtFailureCounter.increment();
                    log.warn("JWT validation failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
                    return Mono.error(e);
                })
                .onErrorResume(e -> {
                    jwtFailureCounter.increment();
                    log.warn("JWT validation failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
                    return Mono.just(false);
                })
                .flatMap(proceed -> {
                    if (Boolean.FALSE.equals(proceed) && exchange.getResponse().isCommitted()) {
                        return Mono.empty();
                    }
                    return chain.filter(exchange.mutate().request(reqBuilder.build()).build());
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static String extractString(Claims claims, String key, String fallback) {
        Object val = claims.get(key);
        return (val != null && !val.toString().isBlank()) ? val.toString() : fallback;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
