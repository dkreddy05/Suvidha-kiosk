package com.suvidha.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.billing.dto.response.PaymentConfirmDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotencyService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(redisTemplate, objectMapper);
    }

    @Test
    void getCachedResponse_returnsEmpty_whenKeyIsNull() {
        assertThat(service.getCachedResponse(null)).isEmpty();
    }

    @Test
    void getCachedResponse_returnsEmpty_whenKeyIsBlank() {
        assertThat(service.getCachedResponse("  ")).isEmpty();
    }

    @Test
    void getCachedResponse_returnsEmpty_whenKeyNotInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:billing:nonexistent")).thenReturn(null);

        assertThat(service.getCachedResponse("nonexistent")).isEmpty();
    }

    @Test
    void getCachedResponse_returnsCachedResponse_whenKeyExists() throws Exception {
        PaymentConfirmDTO expected = PaymentConfirmDTO.builder()
                .paymentId("pay_abc123")
                .receiptUrl("/api/v1/billing/receipt/pay_abc123")
                .build();
        String json = objectMapper.writeValueAsString(expected);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get("idempotency:billing:key-1")).thenReturn(json);

        Optional<PaymentConfirmDTO> result = service.getCachedResponse("key-1");

        assertThat(result).isPresent();
        assertThat(result.get().getPaymentId()).isEqualTo("pay_abc123");
        assertThat(result.get().getReceiptUrl()).isEqualTo("/api/v1/billing/receipt/pay_abc123");
    }

    @Test
    void getCachedResponse_returnsEmpty_whenJsonIsCorrupt() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get("idempotency:billing:bad")).thenReturn("{invalid json");

        assertThat(service.getCachedResponse("bad")).isEmpty();
    }

    @Test
    void cacheResponse_doesNothing_whenKeyIsNull() {
        service.cacheResponse(null, PaymentConfirmDTO.builder().build());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void cacheResponse_doesNothing_whenKeyIsBlank() {
        service.cacheResponse("  ", PaymentConfirmDTO.builder().build());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void cacheResponse_storesInRedisWith24hTTL() throws Exception {
        PaymentConfirmDTO response = PaymentConfirmDTO.builder()
                .paymentId("pay_xyz789")
                .receiptUrl("/api/v1/billing/receipt/pay_xyz789")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.cacheResponse("new-key", response);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("idempotency:billing:new-key");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(24));

        PaymentConfirmDTO deserialized = objectMapper.readValue(valueCaptor.getValue(), PaymentConfirmDTO.class);
        assertThat(deserialized.getPaymentId()).isEqualTo("pay_xyz789");
    }
}
