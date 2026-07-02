package com.suvidha.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts HTTP requests to log request lifecycle events with MDC context.
 * Registered via SuvidhaLoggingAutoConfiguration.
 */
@org.springframework.stereotype.Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String ATTR_START_TIME = "requestStartTime";
    private static final String MDC_HTTP_METHOD = "httpMethod";
    private static final String MDC_HTTP_URL = "httpUrl";
    private static final String MDC_HTTP_DURATION_MS = "httpDurationMs";
    private static final String MDC_HTTP_STATUS = "httpStatus";
    private static final String MDC_HTTP_REMOTE_IP = "httpRemoteIp";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute(ATTR_START_TIME, startTime);

        MDC.put(MDC_HTTP_METHOD, request.getMethod());
        MDC.put(MDC_HTTP_URL, request.getRequestURI());
        MDC.put(MDC_HTTP_REMOTE_IP, getClientIp(request));

        log.info("request_started");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute(ATTR_START_TIME);
        long duration = System.currentTimeMillis() - startTime;

        MDC.put(MDC_HTTP_DURATION_MS, String.valueOf(duration));
        MDC.put(MDC_HTTP_STATUS, String.valueOf(response.getStatus()));

        if (response.getStatus() >= 500) {
            log.error("request_failed");
        } else if (response.getStatus() >= 400) {
            log.warn("request_client_error");
        } else {
            log.info("request_completed");
        }

        MDC.remove(MDC_HTTP_METHOD);
        MDC.remove(MDC_HTTP_URL);
        MDC.remove(MDC_HTTP_DURATION_MS);
        MDC.remove(MDC_HTTP_STATUS);
        MDC.remove(MDC_HTTP_REMOTE_IP);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        return request.getRemoteAddr();
    }
}
