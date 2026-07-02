package com.suvidha.gateway.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestIdFilter implements WebFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        
        final String finalRequestId = requestId;
        MDC.put(REQUEST_ID_MDC_KEY, finalRequestId);
        
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, finalRequestId);
        
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(REQUEST_ID_HEADER, finalRequestId))
                .build();
        
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> MDC.remove(REQUEST_ID_MDC_KEY));
    }
}