package com.suvidha.auth.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private ErrorBody error;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(ErrorBody error) {
        this.error = error;
    }

    public static ApiErrorResponse of(String code, String message, String requestId) {
        return new ApiErrorResponse(new ErrorBody(code, message, null, requestId));
    }

    public static ApiErrorResponse of(String code, String message, Map<String, String> fields, String requestId) {
        return new ApiErrorResponse(new ErrorBody(code, message, fields, requestId));
    }

    public ErrorBody getError() {
        return error;
    }

    public void setError(ErrorBody error) {
        this.error = error;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorBody {
        private String code;
        private String message;
        private Map<String, String> fields;
        private String requestId;

        public ErrorBody() {
        }

        public ErrorBody(String code, String message, Map<String, String> fields, String requestId) {
            this.code = code;
            this.message = message;
            this.fields = fields;
            this.requestId = requestId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, String> getFields() {
            return fields;
        }

        public void setFields(Map<String, String> fields) {
            this.fields = fields;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
    }
}
