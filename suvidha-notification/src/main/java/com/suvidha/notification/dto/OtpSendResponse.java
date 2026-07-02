package com.suvidha.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class OtpSendResponse {

    @JsonProperty("notification_id")
    private UUID notificationId;

    private String status;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("sent_at")
    private Instant sentAt;

    public OtpSendResponse() {}

    public OtpSendResponse(UUID notificationId, String status, String phoneNumber, Instant sentAt) {
        this.notificationId = notificationId;
        this.status = status;
        this.phoneNumber = phoneNumber;
        this.sentAt = sentAt;
    }

    public UUID getNotificationId() { return notificationId; }
    public void setNotificationId(UUID notificationId) { this.notificationId = notificationId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
