package com.suvidha.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Authentication filter that trusts gateway-injected headers instead of
 * re-parsing JWTs. The gateway is the sole JWT verifier; downstream services
 * authenticate via X-User-Id / X-User-Role headers that the gateway strips
 * from external requests and re-adds after validation.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final String HEADER_USER_ID   = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String userId = request.getHeader(HEADER_USER_ID);
            String role   = request.getHeader(HEADER_USER_ROLE);

            if (userId != null && !userId.isBlank()) {
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (role != null && !role.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                }
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute("citizenId", userId);
                request.setAttribute("role", role);
                log.debug("Authenticated via gateway headers: userId={}, role={}", userId, role);
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
