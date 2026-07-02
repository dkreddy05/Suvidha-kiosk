package com.suvidha.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * Servlet filter that propagates or generates trace and span IDs for distributed tracing.
 * Runs after CorrelationIdFilter. Registered via SuvidhaLoggingAutoConfiguration.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceContextFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String PARENT_SPAN_ID_HEADER = "X-Span-Id";
    private static final String SESSION_ID_HEADER = "X-Session-Id";

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_SESSION_ID = "sessionId";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        String spanId = generateSpanId();

        String sessionId = request.getHeader(SESSION_ID_HEADER);
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = extractSessionFromCookie(request);
        }

        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_SPAN_ID, spanId);
        if (sessionId != null) {
            MDC.put(MDC_SESSION_ID, sessionId);
        }

        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(PARENT_SPAN_ID_HEADER, spanId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_SPAN_ID);
            MDC.remove(MDC_SESSION_ID);
        }
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

    private String extractSessionFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var cookie : request.getCookies()) {
            if ("SESSION".equalsIgnoreCase(cookie.getName()) ||
                "JSESSIONID".equalsIgnoreCase(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
