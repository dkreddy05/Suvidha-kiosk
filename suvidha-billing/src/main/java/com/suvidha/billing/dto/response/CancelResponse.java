package com.suvidha.billing.dto.response;

public class CancelResponse {
    private String requestId;
    private String status;
    private String message;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CancelResponse(String requestId, String status, String message) {
        this.requestId = requestId;
        this.status = status;
        this.message = message;
    }
}
