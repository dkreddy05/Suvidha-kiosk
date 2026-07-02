package com.suvidha.common.logging;

import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;

public final class MdcUtils {

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_SESSION_ID = "sessionId";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";

    private MdcUtils() {}

    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId);
        }
    }

    public static Optional<String> getUserId() {
        return Optional.ofNullable(MDC.get(MDC_USER_ID));
    }

    public static Optional<String> getSessionId() {
        return Optional.ofNullable(MDC.get(MDC_SESSION_ID));
    }

    public static Optional<String> getCorrelationId() {
        return Optional.ofNullable(MDC.get(MDC_CORRELATION_ID));
    }

    public static Optional<String> getTraceId() {
        return Optional.ofNullable(MDC.get(MDC_TRACE_ID));
    }

    public static Optional<String> getSpanId() {
        return Optional.ofNullable(MDC.get(MDC_SPAN_ID));
    }

    public static Map<String, String> getCopyOfContextMap() {
        return MDC.getCopyOfContextMap();
    }

    public static void setContextMap(Map<String, String> contextMap) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }

    public static void clear() {
        MDC.clear();
    }
}
