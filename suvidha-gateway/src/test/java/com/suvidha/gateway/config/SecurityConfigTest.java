package com.suvidha.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("securityHeadersFilter sets CSP without unsafe-inline")
    void securityHeadersFilter_cspDoesNotContainUnsafeInline() {
        var filter = securityConfig.securityHeadersFilter();
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);
        var chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        String csp = headers.getFirst("Content-Security-Policy");

        assertThat(csp).isNotNull();
        assertThat(csp).doesNotContain("'unsafe-inline'");
    }

    @Test
    @DisplayName("securityHeadersFilter sets nonce-based CSP for scripts and styles")
    void securityHeadersFilter_cspUsesNonceForScriptsAndStyles() {
        var filter = securityConfig.securityHeadersFilter();
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);
        var chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        String csp = headers.getFirst("Content-Security-Policy");
        String nonce = headers.getFirst("X-Content-Security-Policy-Nonce");

        assertThat(csp).isNotNull();
        assertThat(nonce).isNotNull();
        assertThat(nonce).hasSize(24); // 16 bytes base64 = 24 chars
        assertThat(csp).contains("'nonce-" + nonce + "'");
        assertThat(csp).contains("script-src 'self' 'nonce-" + nonce + "'");
        assertThat(csp).contains("style-src 'self' 'nonce-" + nonce + "'");
    }

    @Test
    @DisplayName("securityHeadersFilter generates unique nonces per request")
    void securityHeadersFilter_generatesUniqueNoncesPerRequest() {
        var filter = securityConfig.securityHeadersFilter();
        var chain = mock(WebFilterChain.class);
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

        var exchange1 = MockServerWebExchange.from(MockServerHttpRequest.get("/a").build());
        var exchange2 = MockServerWebExchange.from(MockServerHttpRequest.get("/b").build());

        filter.filter(exchange1, chain).block();
        filter.filter(exchange2, chain).block();

        String nonce1 = exchange1.getResponse().getHeaders().getFirst("X-Content-Security-Policy-Nonce");
        String nonce2 = exchange2.getResponse().getHeaders().getFirst("X-Content-Security-Policy-Nonce");

        assertThat(nonce1).isNotNull();
        assertThat(nonce2).isNotNull();
        assertThat(nonce1).isNotEqualTo(nonce2);
    }

    @Test
    @DisplayName("securityHeadersFilter sets all required security headers")
    void securityHeadersFilter_setsAllSecurityHeaders() {
        var filter = securityConfig.securityHeadersFilter();
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);
        var chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        HttpHeaders headers = exchange.getResponse().getHeaders();

        assertThat(headers.getFirst("Strict-Transport-Security")).contains("max-age=31536000");
        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("X-XSS-Protection")).isEqualTo("1; mode=block");
        assertThat(headers.getFirst("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(headers.getFirst("Content-Security-Policy")).isNotNull();
        assertThat(headers.getFirst("X-Content-Security-Policy-Nonce")).isNotNull();
    }
}
