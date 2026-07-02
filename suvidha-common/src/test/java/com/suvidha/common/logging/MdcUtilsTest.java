package com.suvidha.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MdcUtilsTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void setAndGetUserId() {
        MdcUtils.setUserId("USR-001");
        assertEquals(Optional.of("USR-001"), MdcUtils.getUserId());
    }

    @Test
    void getUserIdReturnsEmptyWhenNotSet() {
        assertEquals(Optional.empty(), MdcUtils.getUserId());
    }

    @Test
    void setUserIdIgnoresNull() {
        MdcUtils.setUserId(null);
        assertEquals(Optional.empty(), MdcUtils.getUserId());
    }

    @Test
    void getTraceIdReturnsEmptyWhenNotSet() {
        assertEquals(Optional.empty(), MdcUtils.getTraceId());
    }

    @Test
    void getSpanIdReturnsEmptyWhenNotSet() {
        assertEquals(Optional.empty(), MdcUtils.getSpanId());
    }

    @Test
    void getSessionIdReturnsEmptyWhenNotSet() {
        assertEquals(Optional.empty(), MdcUtils.getSessionId());
    }

    @Test
    void getCorrelationIdReturnsEmptyWhenNotSet() {
        assertEquals(Optional.empty(), MdcUtils.getCorrelationId());
    }

    @Test
    void getCopyOfContextMapReturnsNullWhenEmpty() {
        MDC.clear();
        assertNull(MdcUtils.getCopyOfContextMap());
    }

    @Test
    void getCopyOfContextMapReturnsMapWhenPopulated() {
        MDC.put("traceId", "abc123");
        MDC.put("correlationId", "def456");

        Map<String, String> copy = MdcUtils.getCopyOfContextMap();
        assertNotNull(copy);
        assertEquals("abc123", copy.get("traceId"));
        assertEquals("def456", copy.get("correlationId"));
    }

    @Test
    void setContextMapRestoresMdc() {
        Map<String, String> context = Map.of(
            "traceId", "trace-1",
            "correlationId", "corr-1",
            "userId", "user-1"
        );

        MdcUtils.setContextMap(context);

        assertEquals("trace-1", MDC.get("traceId"));
        assertEquals("corr-1", MDC.get("correlationId"));
        assertEquals("user-1", MDC.get("userId"));
    }

    @Test
    void setContextMapIgnoresNull() {
        MDC.put("traceId", "original");
        MdcUtils.setContextMap(null);
        assertEquals("original", MDC.get("traceId"));
    }

    @Test
    void clearRemovesAllMdcEntries() {
        MDC.put("traceId", "abc");
        MDC.put("correlationId", "def");

        MdcUtils.clear();

        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("correlationId"));
    }
}
