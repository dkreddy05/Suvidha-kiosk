package com.suvidha.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@ConditionalOnClass(name = "org.springframework.web.filter.OncePerRequestFilter")
@ConditionalOnProperty(name = "suvidha.jwt.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenValidator jwtTokenValidator(JwtProperties properties) {
        return new JwtTokenValidator(
                properties.getPublicKeyUrl(),
                properties.getIssuer(),
                properties.getAudience());
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator) {
        return new JwtAuthenticationFilter(jwtTokenValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtSecurityConfigurer jwtSecurityConfigurer(JwtAuthenticationFilter jwtAuthenticationFilter) {
        return new JwtSecurityConfigurer(jwtAuthenticationFilter);
    }
}
