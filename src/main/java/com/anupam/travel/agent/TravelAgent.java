package com.anupam.travel.agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;

import com.anupam.common.events.ActivityQueryEvent;
import com.anupam.common.events.TravelResponseEvent;
import com.anupam.common.eventbus.EventBusProvider;
import com.google.common.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TravelAgent - Handles travel planning and itinerary queries
 */
public class TravelAgent {

    private static final Logger LOGGER = Logger.getLogger(TravelAgent.class.getName());

    public static final BaseAgent ROOT_AGENT = initAgent();

    private static BaseAgent initAgent() {
        return LlmAgent.builder()
                .name("travel-agent")
                .description("Provides travel planning, itinerary suggestions, and travel recommendations")
                .instruction("""
                        You are a travel planning assistant for the intelligent travel platform.
                        You help users with:
                        - Creating travel itineraries
                        - Finding best routes and transportation options
                        - Suggesting travel destinations
                        - Providing travel tips and advice
                        
                        Use the available tools to provide comprehensive travel information.
                        """)
                .model("gemini-2.5-flash")
                .tools(FunctionTool.create(TravelAgent.class, "planItinerary"))
                .build();
    }

    // Register as EventBus subscriber
    static {
        EventBusProvider.getEventBus().register(new TravelAgent());
    }

    @Subscribe
    public void onActivityQuery(ActivityQueryEvent event) {
        if (event == null) return;
        String city = event.getCity();
        String corrId = event.getCorrelationId();
        try {
            LOGGER.info(() -> "[TravelAgent] Received ActivityQueryEvent for city=" + city + " corrId=" + corrId);
            // Reuse existing handleQuery to build a response
            Map<String, Object> result = handleQuery("Plan a trip to " + city);
            Map<String, Object> responseData = new HashMap<>();
            if (result != null && result.containsKey("response")) {
                Object r = result.get("response");
                if (r instanceof Map) {
                    responseData = (Map<String, Object>) r;
                } else {
                    responseData.put("text", r != null ? r.toString() : "");
                }
            }
            EventBusProvider.getEventBus().post(new TravelResponseEvent(corrId, responseData));
            LOGGER.info(() -> "[TravelAgent] Posted TravelResponseEvent for corrId=" + corrId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[TravelAgent] Error processing ActivityQueryEvent for corrId=" + corrId, e);
            // Publish error response
            EventBusProvider.getEventBus().post(new TravelResponseEvent(corrId, new HashMap<>(), "error"));
        }
    }

    @Schema(description = "Plan a travel itinerary for given destination and duration")
    public static Map<String, Object> planItinerary(
            @Schema(name = "destination", description = "The destination city/country") String destination,
            @Schema(name = "duration", description = "Duration of travel in days") int duration) {

        return TravelAgent.handleQuery("Plan " + duration + " day itinerary for " + destination);
    }

    /**
     * Handle travel-related queries
     */
    public static Map<String, Object> handleQuery(String query) {
        Map<String, Object> response = new HashMap<>();
        response.put("agent", "travel-agent");
        response.put("query", query);

        // Extract destination from the query
        String destination = extractDestination(query);
        int duration = extractDuration(query);

        if (destination != null && !destination.isEmpty()) {
            if (duration > 0) {
                response.put("response", planItinerary(destination, duration).get("response"));
            } else {
                response.put("response", "I can help plan a trip to " + destination +
                    ". How many days do you have for this trip?");
            }
        } else {
            response.put("response", generateTravelResponse(query));
        }

        return response;
    }

    /**
     * Extract destination from query
     */
    private static String extractDestination(String query) {
        String lowerQuery = query.toLowerCase();

        String[] patterns = {
            "trip to ",
            "travel to ",
            "visit ",
            "go to ",
            "to "
        };

        for (String pattern : patterns) {
            int index = lowerQuery.indexOf(pattern);
            if (index != -1) {
                String remaining = query.substring(index + pattern.length()).trim();
                String destination = remaining.split("[?!.,;]")[0].trim();
                if (!destination.isEmpty()) {
                    return destination;
                }
            }
        }

        // Try to extract capitalized words
        String[] words = query.split("\\s+");
        for (String word : words) {
            if (word.length() > 2 && Character.isUpperCase(word.charAt(0))) {
                return word.replaceAll("[?!.,;]", "").trim();
            }
        }

        return null;
    }

    /**
     * Extract duration from query
     */
    private static int extractDuration(String query) {
        String[] patterns = {
            "-day ", "-days ", " day ", " days ",
            "-week ", "-weeks ", " week ", " weeks "
        };

        for (String pattern : patterns) {
            int index = query.toLowerCase().indexOf(pattern.toLowerCase());
            if (index != -1) {
                // Extract number before pattern
                String before = query.substring(0, index).trim();
                String[] parts = before.split("\\s+");
                String lastPart = parts[parts.length - 1];

                try {
                    int value = Integer.parseInt(lastPart);
                    // Convert weeks to days if needed
                    if (pattern.contains("week")) {
                        value *= 7;
                    }
                    return value;
                } catch (NumberFormatException e) {
                    // Continue
                }
            }
        }

        return 0;
    }

    private static String generateTravelResponse(String query) {
        // Simple response logic - can be expanded with actual travel API integration
        if (query.toLowerCase().contains("itinerary")) {
            return "I can help you plan a detailed itinerary. Please specify your destination and duration of stay.";
        } else if (query.toLowerCase().contains("route") || query.toLowerCase().contains("transportation")) {
            return "I'll help you find the best transportation options for your journey. What's your starting point and destination?";
        } else if (query.toLowerCase().contains("destination")) {
            return "I can suggest travel destinations based on your preferences. What kind of experience are you looking for?";
        }
        return "I'm your travel planning assistant. How can I help you plan your trip?";
    }
}
