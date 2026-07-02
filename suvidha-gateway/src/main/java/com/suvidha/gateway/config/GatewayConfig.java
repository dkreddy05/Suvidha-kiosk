package com.suvidha.gateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Global configuration for the Gateway, including HTTP client timeouts.
 */
@Configuration
public class GatewayConfig {

    /**
     * Customizes the default Netty HttpClient used by Spring Cloud Gateway
     * to enforce global connection and response timeouts.
     *
     * @return the HttpClientCustomizer bean
     */
    @Bean
    public HttpClientCustomizer httpClientCustomizer() {
        return httpClient -> httpClient
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(30));  // spec: 30-second downstream timeout
    }
}
