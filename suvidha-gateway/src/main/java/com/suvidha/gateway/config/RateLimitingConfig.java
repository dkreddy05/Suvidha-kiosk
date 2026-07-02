package com.suvidha.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Configuration for rate limiting components.
 *
 * <p>Two separate {@link RedisRateLimiter} beans are created:
 * <ul>
 *   <li><b>authenticatedRateLimiter</b> – applied to requests that carry a Bearer token
 *       (default: 100 requests/minute)</li>
 *   <li><b>unauthenticatedRateLimiter</b> – applied to anonymous requests, identified by
 *       client IP (default: 10 requests/minute)</li>
 * </ul>
 *
 * <p>Implementation note: {@code RedisRateLimiter} counts tokens per second internally.
 * We use {@code requestedTokens = 60} so that each request drains 60 tokens from a bucket
 * that replenishes at <em>requestsPerMinute</em> tokens/second, yielding an effective rate
 * of exactly <em>requestsPerMinute</em> requests/minute.
 */
@Configuration
public class RateLimitingConfig {

    @Value("${rate.limit.requestsPerMinute:100}")
    private int authenticatedRequestsPerMinute;

    @Value("${rate.limit.unauthenticatedRequestsPerMinute:10}")
    private int unauthenticatedRequestsPerMinute;

    /**
     * Resolves the rate-limit key.
     * Uses {@code X-User-Id} header (set by JwtClaimsToHeadersFilter after
     * JWT validation) when present (authenticated), otherwise falls back to
     * the client's remote IP address (unauthenticated).
     */
    @Bean
    public KeyResolver customKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }

            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String ip = remoteAddress != null && remoteAddress.getAddress() != null
                    ? remoteAddress.getAddress().getHostAddress()
                    : "unknown-ip";

            return Mono.just(ip);
        };
    }

    /**
     * Rate limiter for authenticated requests.
     * Default: {@code RATE_LIMIT_REQUESTS_PER_MINUTE=100} → 100 req/min with a 20% burst headroom.
     */
    @Primary
    @Bean("authenticatedRateLimiter")
    public RedisRateLimiter authenticatedRateLimiter() {
        // requestedTokens=60 converts the per-second bucket into a per-minute rate.
        // replenishRate = N means N tokens/sec; each request costs 60 → N req/min effective.
        int replenishRate = authenticatedRequestsPerMinute;
        int burstCapacity  = (int) Math.ceil(authenticatedRequestsPerMinute * 1.2) * 60; // 20% burst
        return new RedisRateLimiter(replenishRate, burstCapacity, 60);
    }

    /**
     * Rate limiter for unauthenticated / anonymous requests.
     * Default: {@code RATE_LIMIT_UNAUTHENTICATED_REQUESTS_PER_MINUTE=10} → 10 req/min.
     */
    @Bean("unauthenticatedRateLimiter")
    public RedisRateLimiter unauthenticatedRateLimiter() {
        int replenishRate = unauthenticatedRequestsPerMinute;
        int burstCapacity  = (int) Math.ceil(unauthenticatedRequestsPerMinute * 1.5) * 60; // 50% burst
        return new RedisRateLimiter(replenishRate, burstCapacity, 60);
    }
}
