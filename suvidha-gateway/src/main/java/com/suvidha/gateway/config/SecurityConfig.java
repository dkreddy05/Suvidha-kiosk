package com.suvidha.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.gateway.filter.RequestIdFilter;
import com.suvidha.gateway.jwt.JwtAuthenticationManager;
import com.suvidha.gateway.jwt.JwtServerAuthenticationConverter;
import com.suvidha.gateway.jwt.JwtToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;


import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.security.SecureRandom;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @org.springframework.beans.factory.annotation.Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:3001,http://localhost:5173}")
    private String corsAllowedOriginsStr;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            JwtToken jwtToken,
            RequestIdFilter requestIdFilter,
            ObjectMapper objectMapper) {

        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(new JwtAuthenticationManager(jwtToken));
        jwtFilter.setServerAuthenticationConverter(new JwtServerAuthenticationConverter());
        jwtFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.anyExchange());
        jwtFilter.setAuthenticationFailureHandler(new ServerAuthenticationEntryPointFailureHandler(
                (exchange, ex) -> writeError(exchange, HttpStatus.UNAUTHORIZED,
                        "Unauthorized", "Invalid or expired token", objectMapper)
        ));

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .addFilterBefore(requestIdFilter, SecurityWebFiltersOrder.FIRST)
                .addFilterBefore(securityHeadersFilter(), SecurityWebFiltersOrder.LAST)
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((exchange, ex) ->
                                writeError(exchange, HttpStatus.UNAUTHORIZED,
                                        "Unauthorized", "Invalid or expired token", objectMapper))
                        .accessDeniedHandler((exchange, ex) ->
                                writeError(exchange, HttpStatus.FORBIDDEN,
                                        "Forbidden", "Insufficient permissions", objectMapper))
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/health", "/actuator/health", "/actuator/prometheus").permitAll()
                        .pathMatchers("/actuator/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST,
                                "/api/auth/send-otp",  "/api/v1/auth/send-otp",
                                "/api/auth/otp",       "/api/v1/auth/otp",
                                "/api/auth/verify-otp","/api/v1/auth/verify-otp",
                                "/api/auth/refresh-token","/api/v1/auth/refresh-token",
                                "/api/auth/register",  "/api/v1/auth/register").permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/api/auth/public-key","/api/v1/auth/public-key").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }



    @Bean
    public WebFilter securityHeadersFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String nonce = generateNonce();

            exchange.getResponse().getHeaders().add("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");
            exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
            exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
            exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
            exchange.getResponse().getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
            exchange.getResponse().getHeaders().add("X-Content-Security-Policy-Nonce", nonce);
            exchange.getResponse().getHeaders().add("Content-Security-Policy",
                    "default-src 'self'; script-src 'self' 'nonce-" + nonce + "'; style-src 'self' 'nonce-" + nonce + "'; img-src 'self' data:; font-src 'self'; connect-src 'self'");
            return chain.filter(exchange);
        };
    }

    private static String generateNonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status,
                         String error, String message, ObjectMapper objectMapper) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String origin = exchange.getRequest().getHeaders().getFirst("Origin");
        Set<String> allowedOrigins = Set.of(corsAllowedOriginsStr.split(","));
        if (origin != null && allowedOrigins.contains(origin.trim())) {
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", origin);
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Credentials", "true");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Request-Id, X-Kiosk-Id");
        }
        Map<String, Object> body = Map.of(
                "status",  status.value(),
                "error",   error,
                "message", message
        );
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return exchange.getResponse().setComplete();
        }
    }
}
