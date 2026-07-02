package com.suvidha.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuth extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuth.class);

    private final JwtToken jwtToken;

    public JwtAuth(JwtToken jwtToken) {
        this.jwtToken = jwtToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7).trim();

            try {
                Claims claims = jwtToken.validate(token);

                if (jwtToken.isBlacklisted(token)) {
                    log.warn("JWT is blacklisted (logout): jti={}", claims.getId());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token revoked");
                    return;
                }

                String citizenId = claims.getSubject();

                String type = claims.get("type", String.class);
                if (!"access".equals(type)) {
                    throw new RuntimeException("Invalid token type");
                }

                String role = claims.get("role", String.class);
                if (role == null) {
                    Object rolesRaw = claims.get("roles");
                    if (rolesRaw instanceof List<?> rolesList && !rolesList.isEmpty()) {
                        role = rolesList.get(0).toString();
                    }
                }
                List<SimpleGrantedAuthority> authorities = role == null
                        ? List.of()
                        : List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

                if (citizenId != null &&
                        SecurityContextHolder.getContext().getAuthentication() == null) {

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            citizenId,
                            null,
                            authorities);

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

                request.setAttribute("claims", claims);

            } catch (ExpiredJwtException e) {
                log.warn("JWT expired: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token expired");
                return;

            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}