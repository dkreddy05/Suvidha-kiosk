package com.suvidha.common.config;

import com.suvidha.common.logging.RequestLoggingInterceptor;
import com.suvidha.common.filter.CorrelationIdFilter;
import com.suvidha.common.filter.TraceContextFilter;
import com.suvidha.common.filter.SecurityHeaderFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.HandlerInterceptor")
public class SuvidhaLoggingAutoConfiguration implements WebMvcConfigurer {

    @Bean
    public RequestLoggingInterceptor requestLoggingInterceptor() {
        return new RequestLoggingInterceptor();
    }

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public TraceContextFilter traceContextFilter() {
        return new TraceContextFilter();
    }

    @Bean
    public SecurityHeaderFilter securityHeaderFilter() {
        return new SecurityHeaderFilter();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor())
                .order(1);
    }
}
