package com.suvidha.common.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void generatesCorrelationIdWhenHeaderMissing() throws Exception {
        AtomicReference<String> mdcDuring = new AtomicReference<>();
        FilterChain chain = capturingChain(mdcDuring);

        filter.doFilterInternal(request, response, chain);

        String correlationId = response.getHeader("X-Correlation-ID");
        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());
        assertEquals(correlationId, mdcDuring.get());
    }

    @Test
    void propagatesCorrelationIdFromHeader() throws Exception {
        String existingId = "550e8400-e29b-41d4-a716-446655440000";
        request.addHeader("X-Correlation-ID", existingId);
        AtomicReference<String> mdcDuring = new AtomicReference<>();
        FilterChain chain = capturingChain(mdcDuring);

        filter.doFilterInternal(request, response, chain);

        assertEquals(existingId, response.getHeader("X-Correlation-ID"));
        assertEquals(existingId, mdcDuring.get());
    }

    @Test
    void generatesNewIdWhenHeaderBlank() throws Exception {
        request.addHeader("X-Correlation-ID", "   ");

        filter.doFilterInternal(request, response, mockFilterChain());

        String correlationId = response.getHeader("X-Correlation-ID");
        assertNotNull(correlationId);
        assertNotEquals("   ", correlationId);
    }

    @Test
    void clearsMdcAfterFilter() throws Exception {
        filter.doFilterInternal(request, response, mockFilterChain());

        assertNull(MDC.get("correlationId"));
    }

    @Test
    void generatedIdIsValidUuid() throws Exception {
        filter.doFilterInternal(request, response, mockFilterChain());

        String correlationId = response.getHeader("X-Correlation-ID");
        assertNotNull(correlationId);
        assertDoesNotThrow(() -> java.util.UUID.fromString(correlationId));
    }

    private FilterChain capturingChain(AtomicReference<String> capture) throws Exception {
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            capture.set(MDC.get("correlationId"));
            return null;
        }).when(chain).doFilter(any(), any());
        return chain;
    }

    private FilterChain mockFilterChain() throws Exception {
        return mock(FilterChain.class);
    }
}
