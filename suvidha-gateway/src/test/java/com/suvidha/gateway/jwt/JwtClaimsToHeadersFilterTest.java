package com.suvidha.gateway.jwt;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtClaimsToHeadersFilterTest {

    private JwtToken jwtToken;
    private JwtClaimsToHeadersFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtToken = mock(JwtToken.class);
        filter = new JwtClaimsToHeadersFilter(jwtToken, new SimpleMeterRegistry());
        chain = mock(GatewayFilterChain.class);
        lenient().when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void shouldForwardAllClaimsAsHeaders() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("9876543210");
        when(claims.get("citizenId")).thenReturn("CIT123");
        when(claims.get("name")).thenReturn("John Doe");
        when(claims.get("mobile")).thenReturn("9876543210");
        when(claims.get("role")).thenReturn("USER");
        when(claims.get("lang")).thenReturn("en");
        when(claims.getId()).thenReturn("test-jti");

        when(jwtToken.validate(anyString())).thenReturn(claims);
        lenient().when(jwtToken.isBlacklisted(anyString())).thenReturn(Mono.just(false));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerWebExchange mutatedExchange = captor.getValue();
        HttpHeaders headers = mutatedExchange.getRequest().getHeaders();

        assertEquals("CIT123", headers.getFirst(JwtClaimsToHeadersFilter.HEADER_CITIZEN_ID));
        assertEquals("John Doe", headers.getFirst(JwtClaimsToHeadersFilter.HEADER_CITIZEN_NAME));
        assertEquals("9876543210", headers.getFirst(JwtClaimsToHeadersFilter.HEADER_CITIZEN_MOBILE));
        assertEquals("USER", headers.getFirst(JwtClaimsToHeadersFilter.HEADER_USER_ROLE));
        assertEquals("en", headers.getFirst(JwtClaimsToHeadersFilter.HEADER_USER_LANGUAGE));
    }

    @Test
    void shouldOmitMissingClaims() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("9876543210");
        when(claims.get("citizenId")).thenReturn(null);
        when(claims.get("name")).thenReturn(null);
        when(claims.get("mobile")).thenReturn(null);
        when(claims.get("role")).thenReturn(null);
        when(claims.get("lang")).thenReturn(null);
        when(claims.getId()).thenReturn("test-jti");

        when(jwtToken.validate(anyString())).thenReturn(claims);
        lenient().when(jwtToken.isBlacklisted(anyString())).thenReturn(Mono.just(false));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerWebExchange mutatedExchange = captor.getValue();
        HttpHeaders headers = mutatedExchange.getRequest().getHeaders();

        // citizenId falls back to subject
        assertEquals("9876543210", headers.getFirst(JwtClaimsToHeadersFilter.HEADER_CITIZEN_ID));
        assertNull(headers.getFirst(JwtClaimsToHeadersFilter.HEADER_CITIZEN_NAME));
        assertNull(headers.getFirst(JwtClaimsToHeadersFilter.HEADER_CITIZEN_MOBILE));
        assertNull(headers.getFirst(JwtClaimsToHeadersFilter.HEADER_USER_ROLE));
        assertNull(headers.getFirst(JwtClaimsToHeadersFilter.HEADER_USER_LANGUAGE));
    }

    @Nested
    @DisplayName("Blacklisted token rejection")
    class BlacklistTests {

        @Test
        @DisplayName("should reject blacklisted token with 401")
        void shouldRejectBlacklistedToken() {
            Claims claims = mock(Claims.class);
            when(claims.getId()).thenReturn("blacklisted-jti");

            when(jwtToken.validate(anyString())).thenReturn(claims);
            when(jwtToken.isBlacklisted(anyString())).thenReturn(Mono.just(true));

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer blacklisted.token.here")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> filter.filter(exchange, chain).block());
        }

        @Test
        @DisplayName("should forward non-blacklisted token normally")
        void shouldForwardNonBlacklistedToken() {
            Claims claims = mock(Claims.class);
            when(claims.getSubject()).thenReturn("citizen-1");
            when(claims.get("citizenId")).thenReturn("citizen-1");
            when(claims.getId()).thenReturn("valid-jti");

            when(jwtToken.validate(anyString())).thenReturn(claims);
            when(jwtToken.isBlacklisted(anyString())).thenReturn(Mono.just(false));

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            verify(chain).filter(any(ServerWebExchange.class));
        }
    }
}
