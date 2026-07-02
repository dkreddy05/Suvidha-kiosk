package com.suvidha.common.filter;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;

public class ReactiveTraceContextFilter implements WebFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String SESSION_ID_HEADER = "X-Session-Id";

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_SESSION_ID = "sessionId";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String traceId = headers.getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        String spanId = generateSpanId();

        String correlationId = headers.getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = java.util.UUID.randomUUID().toString();
        }

        String sessionId = headers.getFirst(SESSION_ID_HEADER);

        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_SPAN_ID, spanId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        if (sessionId != null) {
            MDC.put(MDC_SESSION_ID, sessionId);
        }

        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        exchange.getResponse().getHeaders().set(SPAN_ID_HEADER, spanId);
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    MDC.remove(MDC_TRACE_ID);
                    MDC.remove(MDC_SPAN_ID);
                    MDC.remove(MDC_CORRELATION_ID);
                    MDC.remove(MDC_SESSION_ID);
                });
    }

    private String generateTraceId() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private String generateSpanId() {
        byte[] bytes = new byte[8];
        SECURE_RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
