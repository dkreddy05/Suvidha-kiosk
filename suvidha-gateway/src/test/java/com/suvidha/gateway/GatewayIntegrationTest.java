package com.suvidha.gateway;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Mono;

import java.util.Collections;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "gateway.ratelimit.replenishRate=20",
        "gateway.ratelimit.burstCapacity=40",
        "gateway.ratelimit.requestedTokens=1",
        "spring.cloud.gateway.routes[0].id=test-route",
        "spring.cloud.gateway.routes[0].uri=http://localhost:8089",
        "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/test/**",
        "spring.cloud.gateway.httpclient.response-timeout=2000",
        "spring.cloud.gateway.routes[0].metadata.response-timeout=2000"
    }
)
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean(name = "authenticatedRateLimiter")
    private RedisRateLimiter authenticatedRateLimiter;

    @MockBean(name = "unauthenticatedRateLimiter")
    private RedisRateLimiter unauthenticatedRateLimiter;

    @MockBean
    private com.suvidha.gateway.jwt.JwtToken jwtToken;

    @Test
    void shouldReturn504WhenDownstreamTimesOut() {
        // WireMock setup: delay response beyond timeout
        stubFor(get(urlEqualTo("/api/test/timeout"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(15000)));

        when(authenticatedRateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RedisRateLimiter.Response(true, Collections.emptyMap())));
        when(unauthenticatedRateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RedisRateLimiter.Response(true, Collections.emptyMap())));

        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn("test-jti");
        when(claims.getSubject()).thenReturn("9876543210");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(jwtToken.validateAsync(anyString())).thenReturn(Mono.just(claims));
        when(jwtToken.isBlacklisted(anyString())).thenReturn(Mono.just(false));

        webClient.get().uri("/api/test/timeout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.status").isEqualTo(504)
                .jsonPath("$.error").isEqualTo("Gateway Timeout");
    }
}
