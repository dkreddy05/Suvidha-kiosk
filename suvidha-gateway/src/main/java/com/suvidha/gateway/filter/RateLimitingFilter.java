package com.suvidha.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Value("${rate.limit.enabled:true}")
    private boolean rateLimitEnabled;

    private final RedisRateLimiter authenticatedRateLimiter;
    private final RedisRateLimiter unauthenticatedRateLimiter;
    private final KeyResolver keyResolver;
    private final ObjectMapper objectMapper;
    private final Counter rateLimitExceededCounter;

    public RateLimitingFilter(
            @Qualifier("authenticatedRateLimiter") RedisRateLimiter authenticatedRateLimiter,
            @Qualifier("unauthenticatedRateLimiter") RedisRateLimiter unauthenticatedRateLimiter,
            KeyResolver keyResolver,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.authenticatedRateLimiter = authenticatedRateLimiter;
        this.unauthenticatedRateLimiter = unauthenticatedRateLimiter;
        this.keyResolver = keyResolver;
        this.objectMapper = objectMapper;
        this.rateLimitExceededCounter = Counter.builder("gateway.ratelimit.exceeded")
                .description("Number of requests rejected due to rate limiting")
                .register(meterRegistry);
    }

    private static class RateLimiterUnreachableException extends RuntimeException {
        public RateLimiterUnreachableException(Throwable cause) {
            super(cause);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!rateLimitEnabled) return chain.filter(exchange);

        String path = exchange.getRequest().getURI().getPath();
        boolean hasToken = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION) != null;
        RedisRateLimiter limiter = hasToken ? authenticatedRateLimiter : unauthenticatedRateLimiter;

        return keyResolver.resolve(exchange).flatMap(key ->
            limiter.isAllowed(path, key)
                .onErrorMap(throwable -> new RateLimiterUnreachableException(throwable))
                .flatMap(response -> {
                    if (response.isAllowed()) {
                        return chain.filter(exchange);
                    } else {
                        return handleRateLimitExceeded(exchange, response, key, path);
                    }
                })
                .onErrorResume(RateLimiterUnreachableException.class, throwable -> {
                    log.error("Redis unreachable — failing closed for rate limiter: {}", throwable.getCause().getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    Map<String, Object> body = Map.of(
                            "status", 503,
                            "error", "Service Unavailable",
                            "message", "Rate limiting service unavailable. Please try again later."
                    );
                    try {
                        byte[] bytes = objectMapper.writeValueAsBytes(body);
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                        return exchange.getResponse().writeWith(Mono.just(buffer));
                    } catch (JsonProcessingException e) {
                        return Mono.error(e);
                    }
                })
        );
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange,
                                                RedisRateLimiter.Response response,
                                                String key, String path) {
        rateLimitExceededCounter.increment();
        log.warn("Rate limit exceeded: key={} path={} remaining={}",
                key, path, response.getHeaders().get(RedisRateLimiter.REMAINING_HEADER));

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        long retryAfter = 1;
        exchange.getResponse().getHeaders().add(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));

        Map<String, Object> body = Map.of(
                "status", 429,
                "error", "Too Many Requests",
                "message", "Rate limit exceeded. Retry after " + retryAfter + " seconds",
                "retryAfter", retryAfter
        );
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
