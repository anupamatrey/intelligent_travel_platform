package com.anupam.risk.agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RiskAssessmentAgent - Handles safety and risk assessment for travel
 */
public class RiskAssessmentAgent {

    private static final Logger LOGGER = Logger.getLogger(RiskAssessmentAgent.class.getName());

    public static final BaseAgent ROOT_AGENT = initAgent();

    private static BaseAgent initAgent() {
        return LlmAgent.builder()
                .name("risk-agent")
                .description("Provides risk assessment, safety information, and travel advisories")
                .instruction("""
                        You are a risk assessment and safety advisor for the intelligent travel platform.
                        You help users with:
                        - Assessing safety levels of travel destinations
                        - Providing health and safety recommendations
                        - Offering travel advisories and warnings
                        - Suggesting safety precautions and tips
                        - Information about travel insurance and documents
                        
                        Prioritize user safety and provide accurate, helpful information.
                        """)
                .model("gemini-2.5-flash")
                .tools(FunctionTool.create(RiskAssessmentAgent.class, "assessRisk"))
                .build();
    }

    @Schema(description = "Assess safety and risks for a travel destination")
    public static Map<String, Object> assessRisk(
            @Schema(name = "destination", description = "The destination city/country") String destination,
            @Schema(name = "riskCategory", description = "Type of risk to assess (health, security, weather, etc.)") String riskCategory) {

        LOGGER.info(() -> "RiskAssessmentAgent.assessRisk called for destination=" + destination + " category=" + riskCategory);
        return RiskAssessmentAgent.handleQuery("Assess " + riskCategory + " risk for " + destination);
    }

    /**
     * Handle risk-related queries
     */
    public static Map<String, Object> handleQuery(String query) {
        LOGGER.info(() -> "RiskAssessmentAgent.handleQuery called with query=" + query);
        Map<String, Object> response = new HashMap<>();
        response.put("agent", "risk-agent");
        response.put("query", query);

        try {
            // Extract destination and risk category from the query
            String destination = extractDestination(query);
            String riskCategory = extractRiskCategory(query);

            if (destination != null && !destination.isEmpty()) {
                response.put("response", assessRisk(destination,
                    riskCategory != null ? riskCategory : "general").get("response"));
            } else {
                response.put("response", generateRiskResponse(query));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in RiskAssessmentAgent.handleQuery", e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * Extract destination from query
     */
    private static String extractDestination(String query) {
        String lowerQuery = query.toLowerCase();

        String[] patterns = {
            "to ",
            "for ",
            "in "
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
     * Extract risk category from query
     */
    private static String extractRiskCategory(String query) {
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("health")) return "health";
        if (lowerQuery.contains("security") || lowerQuery.contains("safe") || lowerQuery.contains("danger")) return "security";
        if (lowerQuery.contains("weather")) return "weather";
        if (lowerQuery.contains("insurance") || lowerQuery.contains("document")) return "insurance";
        if (lowerQuery.contains("vaccine") || lowerQuery.contains("disease")) return "health";

        return "general";
    }

    private static String generateRiskResponse(String query) {
        // Simple response logic - can be expanded with actual risk data integration
        if (query.toLowerCase().contains("health")) {
            return "For health-related risks, I can provide information about vaccinations, medical facilities, and health precautions for your destination.";
        } else if (query.toLowerCase().contains("security") || query.toLowerCase().contains("safe")) {
            return "I can assess the security situation and provide safety recommendations. Which destination are you concerned about?";
        } else if (query.toLowerCase().contains("weather")) {
            return "For weather-related risks, I can inform you about seasonal hazards, extreme weather, and how to prepare.";
        } else if (query.toLowerCase().contains("insurance") || query.toLowerCase().contains("document")) {
            return "I can guide you on travel insurance options and necessary travel documents for your destination.";
        }
        return "I'm your travel safety advisor. What risk assessment would you like? (health, security, weather, insurance, etc.)";
    }
}

