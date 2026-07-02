package com.suvidha.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayErrorHandlerTest {

    private GatewayErrorHandler errorHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        errorHandler = new GatewayErrorHandler(objectMapper);
    }

    @Test
    void shouldReturn502OnConnectException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        errorHandler.handle(exchange, new ConnectException("Connection refused")).block();

        assertEquals(HttpStatus.BAD_GATEWAY, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldReturn504OnTimeoutException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        errorHandler.handle(exchange, new TimeoutException("Read timed out")).block();

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldReturn502OnServiceUnavailableResponseStatus() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        errorHandler.handle(exchange, new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).block();

        assertEquals(HttpStatus.BAD_GATEWAY, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldPassThroughOtherResponseStatusExceptions() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        errorHandler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND)).block();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }
}
