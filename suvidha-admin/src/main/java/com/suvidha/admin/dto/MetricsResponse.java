package com.suvidha.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricsResponse<T> {

    private T data;
    private Instant timestamp;
    private String period;

    public MetricsResponse() {}

    public MetricsResponse(T data, String period) {
        this.data = data;
        this.timestamp = Instant.now();
        this.period = period;
    }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}
