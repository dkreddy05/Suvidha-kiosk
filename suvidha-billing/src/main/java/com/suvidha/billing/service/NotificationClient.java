package com.suvidha.billing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NotificationClient(RestTemplate restTemplate,
            @Value("${suvidha.notifications.base-url:http://localhost:8085}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void sendOtpSms(String mobile, String otp, String referenceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = Map.of(
                "mobile", mobile,
                "otp", otp,
                "referenceId", referenceId);
        restTemplate.postForEntity(baseUrl + "/api/notifications/sms/otp", new HttpEntity<>(payload, headers), Void.class);
    }
}
