package com.anupam.common.events;

import java.io.Serializable;

/**
 * Event published by OrchestratorAgent when user asks about activities
 * Subscribers: TravelAgent, WeatherAgent
 */
public class ActivityQueryEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String city;
    private String correlationId;
    private long timestamp;

    public ActivityQueryEvent(String city, String correlationId) {
        this.city = city;
        this.correlationId = correlationId;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getCity() {
        return city;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ActivityQueryEvent{" +
                "city='" + city + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

