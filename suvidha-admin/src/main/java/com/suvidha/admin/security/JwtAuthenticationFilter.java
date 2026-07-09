package com.suvidha.admin.security;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    Claims claims = jwtUtil.parseClaims(token);
                    String citizenId = jwtUtil.extractCitizenId(claims);
                    String role = jwtUtil.extractRole(claims);

                    if (citizenId != null && !citizenId.isBlank()) {
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        if (role != null && !role.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        }
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                citizenId, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        request.setAttribute("citizenId", citizenId);
                        request.setAttribute("role", role);
                    }
                } catch (ExpiredJwtException ex) {
                    log.warn("JWT expired: {}", ex.getMessage());
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
                    return;
                } catch (Exception ex) {
                    log.warn("JWT authentication failed: {}", ex.getMessage());
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
        Map<String, Object> body = Map.of(
                "error", status == 401 ? "Unauthorized" : "Forbidden",
                "message", message,
                "timestamp", Instant.now().toString());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
