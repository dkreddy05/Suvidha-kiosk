package com.suvidha.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds common tags to all Micrometer metrics for consistent identification
 * across Prometheus dashboards.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name:suvidha}") String applicationName) {
        return registry -> registry.config().commonTags(
                "application", applicationName);
    }
}
