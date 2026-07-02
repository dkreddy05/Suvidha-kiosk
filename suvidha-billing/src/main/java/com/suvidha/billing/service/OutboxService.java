package com.suvidha.billing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.billing.entity.OutboxEvent;
import com.suvidha.billing.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();
            outboxEventRepository.save(event);
            log.debug("Recorded outbox event: {} {} {}", aggregateType, aggregateId, eventType);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new IllegalStateException("Failed to record outbox event", e);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordOtpSent(String mobile, String otp, String citizenId, String consumerNo) {
        Map<String, String> payload = Map.of(
                "mobile", mobile,
                "citizenId", citizenId,
                "consumerNo", consumerNo,
                "otp", otp
        );
        recordEvent("OTP", mobile, "OTP_SENT", payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordAccountLinked(String citizenId, String consumerNo, String serviceType) {
        Map<String, String> payload = Map.of(
                "citizenId", citizenId,
                "consumerNo", consumerNo,
                "serviceType", serviceType
        );
        recordEvent("ServiceAccount", consumerNo, "ACCOUNT_LINKED", payload);
    }
}
