package com.anupam.common.events;

import java.io.Serializable;
import java.util.Map;

/**
 * Event published by WeatherAgent in response to ActivityQueryEvent
 * Includes weather data
 */
public class WeatherResponseEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String correlationId;
    private Map<String, Object> data;
    private long timestamp;
    private String status;

    public WeatherResponseEvent(String correlationId, Map<String, Object> data) {
        this.correlationId = correlationId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.status = "success";
    }

    public WeatherResponseEvent(String correlationId, Map<String, Object> data, String status) {
        this.correlationId = correlationId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.status = status;
    }

    // Getters
    public String getCorrelationId() {
        return correlationId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "WeatherResponseEvent{" +
                "correlationId='" + correlationId + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", data_keys=" + (data != null ? data.keySet() : "null") +
                '}';
    }
}

