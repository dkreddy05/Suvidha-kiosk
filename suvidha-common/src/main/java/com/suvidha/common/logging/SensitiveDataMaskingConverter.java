package com.suvidha.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

public class SensitiveDataMaskingConverter extends ClassicConverter {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("(?i)[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern MOBILE_PATTERN =
        Pattern.compile("(?i)(mobile|phone|tel)(\\s*[:=]\\s*)(\\+?\\d[\\d\\s-]{7,})");
    private static final Pattern AADHAAR_PATTERN =
        Pattern.compile("(?i)(aadhaar|ssn|pan)(\\s*[:=]?\\s*)(\\d{4})\\s*[- ]?\\s*(\\d{4})\\s*[- ]?\\s*(\\d{4})\\s*[- ]?\\s*(\\d{4})");
    private static final Pattern CREDENTIAL_PATTERN =
        Pattern.compile("(?i)(password|secret|token|api[_-]?key|authorization)(\\s*[:=]\\s*)\\S+(?:\\s+\\S+)?");
    private static final Pattern CARD_PATTERN =
        Pattern.compile("(?i)(card|credit|debit|ccn|card[_-]?number)(\\s*[:=]?\\s*)(\\d{4})\\s*[- ]?\\s*(\\d{4})\\s*[- ]?\\s*(\\d{4})\\s*[- ]?\\s*(\\d{4})");

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return "";
        }

        String masked = message;
        masked = CREDENTIAL_PATTERN.matcher(masked).replaceAll("$1=***REDACTED***");
        masked = AADHAAR_PATTERN.matcher(masked).replaceAll("$1$2$3-XXXX-XXXX-$6");
        masked = CARD_PATTERN.matcher(masked).replaceAll("$1$2****-****-****-$6");
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("***@***.***");
        masked = MOBILE_PATTERN.matcher(masked).replaceAll("$1$2+XX-XXXXX-***");
        return masked;
    }
}
