package com.suvidha.billing.service;

import com.suvidha.billing.entity.OutboxEvent;
import com.suvidha.billing.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the outbox table for pending events and delegates each event
 * to {@link OutboxEventProcessor} for processing in its own transaction.
 *
 * The polling query uses {@code FOR UPDATE SKIP LOCKED} so multiple
 * instances can safely run concurrently without double-processing.
 */
@Service
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    private static final int DEFAULT_BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventProcessor outboxEventProcessor;

    public OutboxProcessor(OutboxEventRepository outboxEventRepository,
                          OutboxEventProcessor outboxEventProcessor) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventProcessor = outboxEventProcessor;
    }

    /**
     * Fetches a batch of pending events with row-level locking (SKIP LOCKED),
     * then processes each event in its own REQUIRES_NEW transaction so that
     * one failure doesn't roll back the entire batch.
     */
    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:500}", initialDelayString = "${outbox.initial-delay-ms:5000}")
    @Transactional
    public void processOutboxEvents() {
        try {
            var pendingEvents = outboxEventRepository.findPendingEventsForUpdate(DEFAULT_BATCH_SIZE);

            if (pendingEvents.isEmpty()) {
                return;
            }

            log.debug("Processing {} pending outbox events", pendingEvents.size());
            for (OutboxEvent event : pendingEvents) {
                try {
                    outboxEventProcessor.processEvent(event);
                } catch (Exception e) {
                    // processEvent should handle its own errors, but guard against unexpected ones
                    log.error("Unexpected error processing outbox event {}: {}",
                            event.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in outbox processor", e);
        }
    }

    @Scheduled(cron = "${outbox.cleanup-cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupCompletedEvents() {
        var cutoff = java.time.LocalDateTime.now().minusDays(7);
        int deleted = outboxEventRepository.deleteCompletedEventsBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} completed outbox events older than {}", deleted, cutoff);
        }
    }
}
