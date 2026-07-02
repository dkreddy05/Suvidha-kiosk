package com.suvidha.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SmsStubService {

    private static final Logger log = LoggerFactory.getLogger(SmsStubService.class);

    public void sendSms(String phoneNumber, String message) {
        String timestamp = Instant.now().toString();
        String maskedMessage = message.replaceAll("\\d{4,}", "****");
        String logLine = String.format("[NOTIFICATION] SMS sent to %s: %s at %s", phoneNumber, maskedMessage, timestamp);
        log.info(logLine);
        System.out.println(logLine);
    }
}
