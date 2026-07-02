package com.suvidha.common.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataMaskingConverterTest {

    private SensitiveDataMaskingConverter converter;
    private LoggerContext context;

    @BeforeEach
    void setUp() {
        converter = new SensitiveDataMaskingConverter();
        context = (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    @Test
    void masksEmail() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("User email: john.doe@example.com logged in");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("User email: ***@***.*** logged in", result);
    }

    @Test
    void masksMobile() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("Sending OTP to mobile: +91-98765-43210");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertTrue(result.contains("mobile: +XX-XXXXX-***"));
        assertFalse(result.contains("98765"));
    }

    @Test
    void masksAadhaarWithKeyword() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("Aadhaar: 1234-5678-9012-3456 verified");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("Aadhaar: 1234-XXXX-XXXX-3456 verified", result);
    }

    @Test
    void masksAadhaarWithSpaces() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("SSN: 1234 5678 9012 3456");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("SSN: 1234-XXXX-XXXX-3456", result);
    }

    @Test
    void masksPassword() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("password=SuperSecret123!");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("password=***REDACTED***", result);
    }

    @Test
    void masksToken() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("token: eyJhbGciOiJIUzI1NiJ9.abc123");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("token=***REDACTED***", result);
    }

    @Test
    void masksAuthorizationHeader() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("Authorization: Bearer abc123xyz");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("Authorization=***REDACTED***", result);
    }

    @Test
    void masksApiKey() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("api_key=sk-live-abc123");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("api_key=***REDACTED***", result);
    }

    @Test
    void masksCreditCard() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("card: 4111-1111-1111-1234 charged");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("card: ****-****-****-1234 charged", result);
    }

    @Test
    void caseInsensitiveMasking() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("PASSWORD=mysecret");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("PASSWORD=***REDACTED***", result);

        event = new LoggingEvent();
        event.setMessage("EMAIL=user@test.com");
        event.setLoggerContext(context);

        result = converter.convert(event);
        assertEquals("EMAIL=***@***.***", result);
    }

    @Test
    void masksMultipleSensitiveFields() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("email=test@test.com password=secret123");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertTrue(result.contains("***@***.***"));
        assertTrue(result.contains("password=***REDACTED***"));
    }

    @Test
    void leavesNormalMessageUnchanged() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("Payment processed successfully");
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("Payment processed successfully", result);
    }

    @Test
    void handlesNullMessage() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage(null);
        event.setLoggerContext(context);

        String result = converter.convert(event);
        assertEquals("", result);
    }
}
