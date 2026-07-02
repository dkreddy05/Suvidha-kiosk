package com.suvidha.gateway.jwt;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import reactor.core.publisher.Mono;

public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtToken jwtToken;

    public JwtAuthenticationManager(JwtToken jwtToken) {
        this.jwtToken = jwtToken;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();
        if (token == null || token.isBlank()) {
            return Mono.error(new BadCredentialsException("Missing JWT"));
        }

        try {
            Claims claims = jwtToken.validate(token);
            String mobile = claims.getSubject();
            if (mobile == null || mobile.isBlank()) {
                return Mono.error(new BadCredentialsException("JWT subject missing"));
            }

            String role = claims.get("role", String.class);
            var authorities = role != null 
                ? AuthorityUtils.createAuthorityList("ROLE_" + role) 
                : AuthorityUtils.NO_AUTHORITIES;

            UsernamePasswordAuthenticationToken authed = new UsernamePasswordAuthenticationToken(
                    mobile,
                    token,
                    authorities
            );
            authed.setDetails(claims);
            return Mono.just(authed);
        } catch (Exception e) {
            return Mono.error(new BadCredentialsException("Invalid JWT", e));
        }
    }
}
