package com.anupam.common.messages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Response message wrapper for agent responses
 * Used by agents to send responses back through EventBus
 */
public class AgentResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String agentId;
    private String correlationId;
    private String status;
    private Map<String, Object> data;
    private long timestamp;
    private String errorMessage;

    /**
     * Constructor for successful response
     */
    public AgentResponse(String agentId, String correlationId) {
        this.agentId = agentId;
        this.correlationId = correlationId;
        this.status = "success";
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Constructor for response with status
     */
    public AgentResponse(String agentId, String correlationId, String status) {
        this.agentId = agentId;
        this.correlationId = correlationId;
        this.status = status;
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getAgentId() {
        return agentId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void addData(String key, Object value) {
        this.data.put(key, value);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Utility method to check if response is successful
     */
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(this.status);
    }

    @Override
    public String toString() {
        return "AgentResponse{" +
                "agentId='" + agentId + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", data_keys=" + (data != null ? data.keySet() : "empty") +
                (errorMessage != null ? ", errorMessage='" + errorMessage + '\'' : "") +
                '}';
    }
}

