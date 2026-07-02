package com.suvidha.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Global exception handler to manage downstream routing and timeout exceptions.
 * Ensures consistent JSON responses for 502 Bad Gateway and 504 Gateway Timeout.
 */
@Component
@Order(-2) // Run before DefaultErrorWebExceptionHandler
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String error = "Internal Server Error";
        String message = "An unexpected error occurred.";

        if (ex instanceof ResponseStatusException rse) {
            if (rse.getStatusCode() == HttpStatus.BAD_GATEWAY || rse.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                status = HttpStatus.BAD_GATEWAY;
                error = "Bad Gateway";
                message = "Backend service temporarily unavailable";
            } else {
                status = HttpStatus.valueOf(rse.getStatusCode().value());
                error = status.getReasonPhrase();
                message = rse.getReason();
            }
        } else if (ex instanceof ConnectException || (ex.getCause() != null && ex.getCause() instanceof ConnectException)) {
            status = HttpStatus.BAD_GATEWAY;
            error = "Bad Gateway";
            message = "Backend service temporarily unavailable";
        } else if (ex instanceof TimeoutException || ex instanceof ReadTimeoutException 
                || (ex.getCause() != null && (ex.getCause() instanceof TimeoutException || ex.getCause() instanceof ReadTimeoutException))) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            error = "Gateway Timeout";
            message = "Request timeout";
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = Map.of(
                "status", status.value(),
                "error", error,
                "message", message != null ? message : error
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
