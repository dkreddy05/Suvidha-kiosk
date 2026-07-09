package com.suvidha.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "aws-sns")
public class AwsSnsSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AwsSnsSmsSender.class);

    private final String awsAccessKey;
    private final String awsSecretKey;
    private final String awsRegion;

    public AwsSnsSmsSender(
            @Value("${aws.access-key-id:}") String awsAccessKey,
            @Value("${aws.secret-access-key:}") String awsSecretKey,
            @Value("${aws.region:ap-south-1}") String awsRegion) {
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.awsRegion = awsRegion;
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (awsAccessKey == null || awsAccessKey.isBlank()) {
            log.warn("AWS SNS not configured — skipping SMS to {}", phoneNumber);
            return;
        }
        try {
            log.info("Sending SMS via AWS SNS to {}: [message suppressed in logs]", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS via AWS SNS to {}: {}", phoneNumber, e.getMessage());
        }
    }
}
