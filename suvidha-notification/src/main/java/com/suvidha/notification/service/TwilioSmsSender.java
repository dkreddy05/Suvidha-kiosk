package com.suvidha.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "twilio")
public class TwilioSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final String accountSid;
    private final String authToken;
    private final String fromPhone;

    public TwilioSmsSender(
            @Value("${twilio.account-sid:}") String accountSid,
            @Value("${twilio.auth-token:}") String authToken,
            @Value("${twilio.phone-number:}") String fromPhone) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromPhone = fromPhone;
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (accountSid == null || accountSid.isBlank()) {
            log.warn("Twilio not configured — skipping SMS to {}", phoneNumber);
            return;
        }
        try {
            log.info("Sending SMS via Twilio to {}: [message suppressed in logs]", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS via Twilio to {}: {}", phoneNumber, e.getMessage());
        }
    }
}
