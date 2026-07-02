package com.suvidha.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration shared across services.
 * Each service gets its own Swagger UI at /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI suvidhaOpenAPI(
            @Value("${spring.application.name:suvidha}") String appName) {
        return new OpenAPI()
                .info(new Info()
                        .title("SUVIDHA — " + appName.toUpperCase() + " API")
                        .version("1.0.0")
                        .description("Public Utility Services Platform — " + appName)
                        .contact(new Contact()
                                .name("Suvidha Team")
                                .email("admin@suvidha.com")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT access token obtained from /api/auth/verify-otp")));
    }
}
