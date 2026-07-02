package com.suvidha.billing.security;

import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared utilities for extracting identity and parsing path variables
 * from the security context. Eliminates duplication across controllers.
 */
public final class SecurityUtil {

    private SecurityUtil() {
        // utility class
    }

    /**
     * Extracts the citizenId (principal name) from the current security context.
     *
     * @throws UnauthorizedException if no authentication or principal is present
     */
    public static String currentCitizenId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new UnauthorizedException("Missing citizenId");
        }
        return auth.getName();
    }

    /**
     * Parses and validates a serviceType path variable to its enum value.
     *
     * @throws IllegalArgumentException if null or not a valid ServiceType
     */
    public static ServiceType parseServiceType(String pathValue) {
        if (pathValue == null) {
            throw new IllegalArgumentException("serviceType is required");
        }
        return ServiceType.valueOf(pathValue.trim().toUpperCase());
    }
}
