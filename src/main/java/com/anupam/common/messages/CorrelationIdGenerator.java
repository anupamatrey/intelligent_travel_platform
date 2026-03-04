package com.anupam.common.messages;

import java.util.UUID;

/**
 * Utility class for working with correlation IDs in A2A communication
 * Provides factory methods for creating and managing correlation IDs
 */
public class CorrelationIdGenerator {

    /**
     * Generate a new unique correlation ID
     *
     * @return A new UUID-based correlation ID
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a correlation ID with a prefix
     *
     * @param prefix A prefix to add to the correlation ID
     * @return Correlation ID in format: prefix-uuid
     */
    public static String generateCorrelationId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString();
    }

    /**
     * Create a request-specific correlation ID
     *
     * @param requestType The type of request (e.g., "activity-query")
     * @return Correlation ID for request tracking
     */
    public static String createRequestId(String requestType) {
        return "req-" + requestType + "-" + UUID.randomUUID().toString();
    }

    /**
     * Validate correlation ID format
     *
     * @param correlationId The ID to validate
     * @return true if valid UUID format, false otherwise
     */
    public static boolean isValidCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(correlationId.split("-", 2)[correlationId.contains("-") ? 1 : 0]);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

