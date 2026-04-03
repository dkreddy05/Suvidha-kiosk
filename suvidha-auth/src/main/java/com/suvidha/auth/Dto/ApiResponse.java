package com.suvidha.auth.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String status;
    private String code;
    private String message;
    private T data;
    private Instant timeStamp;

    private ApiResponse() {
        this.timeStamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = "SUCCESS";
        response.message = message;
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = "ERROR";
        response.code = code;
        response.message = message;
        return response;
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = "ERROR";
        response.code = code;
        response.message = message;
        response.data = data;
        return response;
    }

    public String getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

}
