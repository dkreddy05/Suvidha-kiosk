package com.suvidha.common.config;

import com.suvidha.common.filter.ReactiveTraceContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = "org.springframework.web.server.WebFilter")
public class SuvidhaReactiveLoggingAutoConfiguration {

    @Bean
    public ReactiveTraceContextFilter reactiveTraceContextFilter() {
        return new ReactiveTraceContextFilter();
    }
}
