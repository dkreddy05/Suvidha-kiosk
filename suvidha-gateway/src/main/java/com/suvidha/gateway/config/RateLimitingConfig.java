package com.suvidha.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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

    private static final Logger log = LoggerFactory.getLogger(RateLimitingConfig.class);

    @Value("${rate.limit.requestsPerMinute:100}")
    private int authenticatedRequestsPerMinute;

    @Value("${rate.limit.unauthenticatedRequestsPerMinute:10}")
    private int unauthenticatedRequestsPerMinute;

    /**
     * CIDR of the trusted reverse-proxy / ALB subnet (e.g. "10.0.0.0/8").
     * Set via the {@code TRUSTED_PROXY_CIDR} environment variable in production.
     * In local development this defaults to the loopback range so that direct
     * connections still work without an ALB in front.
     *
     * <p><strong>Security contract:</strong> {@code X-Forwarded-For} is only
     * consulted when the TCP peer (the actual socket remote address) falls
     * inside this CIDR.  Requests that arrive directly from the internet keep
     * their raw socket address as the rate-limit key, so clients cannot spoof
     * {@code X-Forwarded-For} to evade rate limiting.
     */
    @Value("${rate.limit.trustedProxyCidr:127.0.0.0/8}")
    private String trustedProxyCidr;

    /**
     * Resolves the rate-limit key.
     *
     * <ol>
     *   <li>Authenticated requests — uses {@code X-User-Id} (injected by
     *       {@code JwtClaimsToHeadersFilter} after JWT validation).  This gives
     *       per-user rate limiting regardless of IP.
     *   <li>Unauthenticated requests behind a trusted proxy — uses the first
     *       (leftmost, i.e. originating-client) value in {@code X-Forwarded-For}.
     *   <li>Unauthenticated requests arriving directly (no trusted proxy) — uses
     *       the raw socket remote address.
     * </ol>
     */
    @Bean
    public KeyResolver customKeyResolver() {
        return exchange -> {
            // --- Authenticated path: rate-limit per user identity, not per IP ---
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }

            // --- Unauthenticated path: derive the real client IP ---
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String peerIp = (remoteAddress != null && remoteAddress.getAddress() != null)
                    ? remoteAddress.getAddress().getHostAddress()
                    : null;

            // Only trust X-Forwarded-For when the immediate TCP peer is within
            // the operator-configured trusted proxy CIDR (e.g. the ALB subnet).
            // This prevents clients from injecting a fake header to spoof their IP.
            if (peerIp != null && isInCidr(peerIp, trustedProxyCidr)) {
                String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
                if (xff != null && !xff.isBlank()) {
                    // XFF is a comma-separated list; the leftmost entry is the originating client.
                    String clientIp = xff.split(",")[0].trim();
                    if (!clientIp.isBlank()) {
                        return Mono.just(clientIp);
                    }
                }
            }

            // Fallback: use the direct socket address (covers local dev + any
            // case where XFF is absent or the peer is not a known proxy).
            return Mono.just(peerIp != null ? peerIp : "unknown-ip");
        };
    }

    /**
     * Checks whether {@code ip} falls inside {@code cidr} (e.g. "10.0.0.0/8").
     * Supports both IPv4 and IPv6 CIDRs.  Returns {@code false} on any parse
     * error so that a misconfigured CIDR never silently grants proxy trust.
     */
    private boolean isInCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) return false;

            InetAddress network  = InetAddress.getByName(parts[0]);
            InetAddress address  = InetAddress.getByName(ip);
            int         prefixLen = Integer.parseInt(parts[1]);

            byte[] netBytes  = network.getAddress();
            byte[] addrBytes = address.getAddress();

            // Address-family mismatch (IPv4 vs IPv6) → not in range.
            if (netBytes.length != addrBytes.length) return false;

            int fullBytes    = prefixLen / 8;
            int remainingBits = prefixLen % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (netBytes[i] != addrBytes[i]) return false;
            }
            if (remainingBits > 0) {
                int mask = 0xFF << (8 - remainingBits) & 0xFF;
                if ((netBytes[fullBytes] & mask) != (addrBytes[fullBytes] & mask)) return false;
            }
            return true;
        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("RateLimitingConfig: could not parse CIDR '{}' — defaulting to deny trust: {}",
                    cidr, e.getMessage());
            return false;
        }
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
