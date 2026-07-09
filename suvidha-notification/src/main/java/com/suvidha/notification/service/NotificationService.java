package com.suvidha.notification.service;

import com.suvidha.notification.dto.NotificationHistoryResponse;
import com.suvidha.notification.dto.OtpSendRequest;
import com.suvidha.notification.dto.OtpSendResponse;
import com.suvidha.notification.entity.Notification;
import com.suvidha.notification.entity.enums.MessageType;
import com.suvidha.notification.entity.enums.NotificationStatus;
import com.suvidha.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SmsSender smsSender;
    private final RateLimiterService rateLimiterService;

    public NotificationService(NotificationRepository notificationRepository,
                                SmsSender smsSender,
                                RateLimiterService rateLimiterService) {
        this.notificationRepository = notificationRepository;
        this.smsSender = smsSender;
        this.rateLimiterService = rateLimiterService;
    }

    @Transactional
    public OtpSendResponse sendOtp(OtpSendRequest request) {
        if (!rateLimiterService.isAllowed(request.getPhoneNumber())) {
            throw new RateLimitExceededException("Rate limit exceeded for phone number: " + request.getPhoneNumber());
        }

        Notification notification = new Notification();
        notification.setCitizenId(request.getCitizenId());
        notification.setPhoneNumber(request.getPhoneNumber());
        notification.setMessageType(MessageType.OTP);
        notification.setMessageContent("Your OTP code is: " + request.getOtpCode());
        notification.setStatus(NotificationStatus.PENDING);
        notification = notificationRepository.save(notification);

        try {
            smsSender.sendSms(request.getPhoneNumber(), "Your OTP code is: ****");
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", request.getPhoneNumber(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
        }

        notificationRepository.save(notification);

        return new OtpSendResponse(
                notification.getNotificationId(),
                notification.getStatus().name().toLowerCase(),
                request.getPhoneNumber(),
                notification.getSentAt());
    }

    public NotificationHistoryResponse getHistory(String citizenId, int page, int size) {
        Page<Notification> notificationPage = notificationRepository
                .findByCitizenIdOrderByCreatedAtDesc(citizenId, PageRequest.of(page, size));

        List<NotificationHistoryResponse.NotificationItem> items = notificationPage.getContent().stream()
                .map(this::toHistoryItem)
                .collect(Collectors.toList());

        NotificationHistoryResponse response = new NotificationHistoryResponse();
        response.setCitizenId(citizenId);
        response.setNotifications(items);
        response.setTotalElements(notificationPage.getTotalElements());
        response.setTotalPages(notificationPage.getTotalPages());
        response.setCurrentPage(page);
        return response;
    }

    private NotificationHistoryResponse.NotificationItem toHistoryItem(Notification n) {
        NotificationHistoryResponse.NotificationItem item = new NotificationHistoryResponse.NotificationItem();
        item.setNotificationId(n.getNotificationId());
        item.setPhoneNumber(maskPhoneNumber(n.getPhoneNumber()));
        item.setMessageType(n.getMessageType().name());
        item.setMessageContent(n.getMessageContent());
        item.setStatus(n.getStatus().name());
        item.setSentAt(n.getSentAt());
        item.setCreatedAt(n.getCreatedAt());
        return item;
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, phone.length() - 4) + "****";
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
