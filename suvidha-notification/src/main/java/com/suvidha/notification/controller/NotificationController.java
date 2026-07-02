package com.suvidha.notification.controller;

import com.suvidha.notification.dto.NotificationHistoryResponse;
import com.suvidha.notification.dto.OtpSendRequest;
import com.suvidha.notification.dto.OtpSendResponse;
import com.suvidha.notification.service.NotificationService;
import com.suvidha.notification.service.NotificationService.RateLimitExceededException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/notifications", "/api/v1/notifications"})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        try {
            OtpSendResponse response = notificationService.sendOtp(request);
            return ResponseEntity.ok(response);
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too Many Requests", "message", e.getMessage()));
        }
    }

    @GetMapping("/history/{citizenId}")
    public ResponseEntity<NotificationHistoryResponse> getHistory(
            @PathVariable String citizenId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String authenticatedCitizenId = getCurrentCitizenId();
        if (!authenticatedCitizenId.equals(citizenId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(notificationService.getHistory(citizenId, page, size));
    }

    private String getCurrentCitizenId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
