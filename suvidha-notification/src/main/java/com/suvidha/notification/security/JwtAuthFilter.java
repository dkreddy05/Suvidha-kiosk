package com.suvidha.notification.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final JwtTokenValidator jwtTokenValidator;

    public JwtAuthFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    Claims claims = jwtTokenValidator.validate(token);
                    String citizenId = claims.getSubject();
                    String type = claims.get("type", String.class);

                    if (!"access".equals(type)) {
                        throw new RuntimeException("Invalid token type");
                    }

                    if (citizenId != null && !citizenId.isBlank()) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(citizenId, null, List.of());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        request.setAttribute("citizenId", citizenId);
                    }
                } catch (ExpiredJwtException ex) {
                    log.warn("JWT expired: {}", ex.getMessage());
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
                    return;
                } catch (Exception ex) {
                    log.warn("JWT validation failed: {}", ex.getMessage());
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "error", "Unauthorized",
                "message", message,
                "timestamp", Instant.now().toString()));
    }
}
