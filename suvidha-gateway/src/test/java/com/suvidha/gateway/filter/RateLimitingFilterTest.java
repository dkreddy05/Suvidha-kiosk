package com.suvidha.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RedisRateLimiter redisRateLimiter;
    private KeyResolver keyResolver;
    private ObjectMapper objectMapper;
    private RateLimitingFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() throws Exception {
        redisRateLimiter = mock(RedisRateLimiter.class);
        keyResolver = mock(KeyResolver.class);
        objectMapper = new ObjectMapper();
        filter = new RateLimitingFilter(redisRateLimiter, redisRateLimiter, keyResolver, objectMapper, new SimpleMeterRegistry());
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        when(keyResolver.resolve(any(ServerWebExchange.class))).thenReturn(Mono.just("test-key"));

        Field rateLimitEnabledField = RateLimitingFilter.class.getDeclaredField("rateLimitEnabled");
        rateLimitEnabledField.setAccessible(true);
        rateLimitEnabledField.setBoolean(filter, true);
    }

    @Test
    @DisplayName("should skip rate limiting for auth routes")
    void shouldSkipAuthRoutes() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verifyNoInteractions(redisRateLimiter);
    }

    @Test
    @DisplayName("should allow request when under rate limit")
    void shouldAllowRequestWhenUnderLimit() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/billing/invoices").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        RedisRateLimiter.Response response = new RedisRateLimiter.Response(true, Collections.emptyMap());
        when(redisRateLimiter.isAllowed(anyString(), anyString())).thenReturn(Mono.just(response));

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("should reject request with 429 when over rate limit")
    void shouldRejectRequestWith429WhenOverLimit() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/billing/invoices").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Map<String, String> headers = Map.of(RedisRateLimiter.REPLENISH_RATE_HEADER, "10");
        RedisRateLimiter.Response response = new RedisRateLimiter.Response(false, headers);
        when(redisRateLimiter.isAllowed(anyString(), anyString())).thenReturn(Mono.just(response));

        filter.filter(exchange, chain).block();

        verifyNoInteractions(chain);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("should fail closed with 503 when Redis is unreachable")
    void shouldFailClosedWhenRedisIsUnreachable() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/billing/invoices").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(redisRateLimiter.isAllowed(anyString(), anyString())).thenReturn(Mono.error(new RuntimeException("Redis down")));

        filter.filter(exchange, chain).block();

        verifyNoInteractions(chain);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("should fail closed with 503 on send-otp when Redis is unreachable")
    void shouldFailClosedOnSendOtpWhenRedisIsUnreachable() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/send-otp").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(redisRateLimiter.isAllowed(anyString(), anyString())).thenReturn(Mono.error(new RuntimeException("Redis down")));

        filter.filter(exchange, chain).block();

        verifyNoInteractions(chain);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }
}
