package com.suvidha.auth.configuration;

import com.suvidha.auth.token.JwtAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuth jwtAuth) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                 .authorizeHttpRequests(auth -> auth
                         .requestMatchers(
                                 "/api/auth/health/**",
                                 "/api/v1/auth/health/**",
                                 "/api/auth/send-otp/**",
                                 "/api/v1/auth/send-otp/**",
                                 "/api/auth/verify-otp/**",
                                 "/api/v1/auth/verify-otp/**",
                                 "/api/auth/register/**",
                                 "/api/v1/auth/register/**")
                         .permitAll()
                         .anyRequest().authenticated())
                .addFilterBefore(jwtAuth, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
