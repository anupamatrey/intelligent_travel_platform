package com.anupam.common.messages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard message envelope for A2A (Agent-to-Agent) communication
 * Provides a standardized format for all inter-agent messages
 */
public class AgentMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String agentId;
    private String messageType;
    private String correlationId;
    private Map<String, Object> payload;
    private long timestamp;
    private String status;

    /**
     * Constructor for creating a new agent message
     *
     * @param agentId The ID of the agent sending the message
     * @param messageType The type of message (e.g., QUERY, RESPONSE)
     * @param correlationId Unique ID to track request-response pairs
     */
    public AgentMessage(String agentId, String messageType, String correlationId) {
        this.agentId = agentId;
        this.messageType = messageType;
        this.correlationId = correlationId;
        this.payload = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
    }

    // Getters and Setters
    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public void addPayloadData(String key, Object value) {
        this.payload.put(key, value);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "AgentMessage{" +
                "agentId='" + agentId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", payload_keys=" + (payload != null ? payload.keySet() : "empty") +
                '}';
    }
}

