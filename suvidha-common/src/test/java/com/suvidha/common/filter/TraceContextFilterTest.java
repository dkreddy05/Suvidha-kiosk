package com.suvidha.common.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TraceContextFilterTest {

    private TraceContextFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new TraceContextFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void generatesTraceIdWhenHeaderMissing() throws Exception {
        AtomicReference<Map<String, String>> mdcDuring = new AtomicReference<>();
        FilterChain chain = capturingChain(mdcDuring);

        filter.doFilterInternal(request, response, chain);

        String traceId = response.getHeader("X-Trace-Id");
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
        assertTrue(traceId.matches("[a-f0-9]{32}"));
        assertEquals(traceId, mdcDuring.get().get("traceId"));
    }

    @Test
    void propagatesTraceIdFromHeader() throws Exception {
        String existingTraceId = "4bf92f50b7d5c1a2e3f4a5b6c7d8e9f0";
        request.addHeader("X-Trace-Id", existingTraceId);
        AtomicReference<Map<String, String>> mdcDuring = new AtomicReference<>();
        FilterChain chain = capturingChain(mdcDuring);

        filter.doFilterInternal(request, response, chain);

        assertEquals(existingTraceId, response.getHeader("X-Trace-Id"));
        assertEquals(existingTraceId, mdcDuring.get().get("traceId"));
    }

    @Test
    void generatesSpanId() throws Exception {
        AtomicReference<Map<String, String>> mdcDuring = new AtomicReference<>();
        FilterChain chain = capturingChain(mdcDuring);

        filter.doFilterInternal(request, response, chain);

        String spanId = response.getHeader("X-Span-Id");
        assertNotNull(spanId);
        assertEquals(16, spanId.length());
        assertTrue(spanId.matches("[a-f0-9]{16}"));
        assertEquals(spanId, mdcDuring.get().get("spanId"));
    }

    @Test
    void extractsSessionIdFromHeader() throws Exception {
        String sessionId = "sess-abc-123";
        request.addHeader("X-Session-Id", sessionId);
        AtomicReference<Map<String, String>> mdcDuring = new AtomicReference<>();
        FilterChain chain = capturingChain(mdcDuring);

        filter.doFilterInternal(request, response, chain);

        assertEquals(sessionId, mdcDuring.get().get("sessionId"));
    }

    @Test
    void extractsSessionIdFromCookie() throws Exception {
        request.setCookies(
            new jakarta.servlet.http.Cookie("SESSION", "cookie-session-id"));
        AtomicReference<Map<String, String>> mdcDuring = new AtomicReference<>();
        FilterChain chain = capturingChain(mdcDuring);

        filter.doFilterInternal(request, response, chain);

        assertEquals("cookie-session-id", mdcDuring.get().get("sessionId"));
    }

    @Test
    void noSessionIdMdcWhenMissing() throws Exception {
        AtomicReference<Map<String, String>> mdcDuring = new AtomicReference<>();
        FilterChain chain = capturingChain(mdcDuring);

        filter.doFilterInternal(request, response, chain);

        assertNull(mdcDuring.get().get("sessionId"));
    }

    @Test
    void clearsAllMdcKeysAfterFilter() throws Exception {
        filter.doFilterInternal(request, response, mockFilterChain());

        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("spanId"));
        assertNull(MDC.get("sessionId"));
    }

    @Test
    void traceIdIsUniquePerRequest() throws Exception {
        filter.doFilterInternal(request, response, mockFilterChain());
        String firstTraceId = response.getHeader("X-Trace-Id");

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, mockFilterChain());
        String secondTraceId = response.getHeader("X-Trace-Id");

        assertNotEquals(firstTraceId, secondTraceId);
    }

    private FilterChain capturingChain(AtomicReference<Map<String, String>> capture) throws Exception {
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            capture.set(MDC.getCopyOfContextMap());
            return null;
        }).when(chain).doFilter(any(), any());
        return chain;
    }

    private FilterChain mockFilterChain() throws Exception {
        return mock(FilterChain.class);
    }
}
