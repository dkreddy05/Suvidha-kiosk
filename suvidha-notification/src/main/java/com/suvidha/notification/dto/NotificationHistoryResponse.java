package com.suvidha.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class NotificationHistoryResponse {

    @JsonProperty("citizen_id")
    private String citizenId;

    private List<NotificationItem> notifications;
    private long totalElements;
    private int totalPages;
    private int currentPage;

    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public List<NotificationItem> getNotifications() { return notifications; }
    public void setNotifications(List<NotificationItem> notifications) { this.notifications = notifications; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public static class NotificationItem {
        @JsonProperty("notification_id")
        private UUID notificationId;

        @JsonProperty("phone_number")
        private String phoneNumber;

        @JsonProperty("message_type")
        private String messageType;

        @JsonProperty("message_content")
        private String messageContent;

        private String status;

        @JsonProperty("sent_at")
        private Instant sentAt;

        @JsonProperty("created_at")
        private Instant createdAt;

        public UUID getNotificationId() { return notificationId; }
        public void setNotificationId(UUID notificationId) { this.notificationId = notificationId; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        public String getMessageContent() { return messageContent; }
        public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getSentAt() { return sentAt; }
        public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }
}
