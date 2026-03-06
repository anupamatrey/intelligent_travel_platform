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
                .description("Provides risk assessment, safety information, travel advisories, peak season warnings, and alternative travel time suggestions")
                .instruction("""
                        You are a risk assessment and safety advisor for the intelligent travel platform.
                        You help users with:
                        - Assessing safety levels of travel destinations
                        - Identifying peak/tourist seasons and associated costs
                        - Suggesting best times to visit with lower costs and fewer crowds
                        - Providing health and safety recommendations
                        - Offering travel advisories and warnings
                        - Suggesting safety precautions and tips
                        - Information about travel insurance and documents
                        - Weather-related risks and hazards
                        
                        For each destination, provide:
                        1. SAFETY LEVEL: Overall safety assessment
                        2. PEAK SEASONS: When tourists visit, pricing, crowds
                        3. BEST TIMES TO VISIT: Alternative times with better conditions and prices
                        4. HEALTH RISKS: Vaccinations, diseases, health facilities
                        5. WEATHER HAZARDS: Seasonal weather risks (monsoons, heat, cold, storms)
                        6. RECOMMENDATIONS: Specific safety precautions and insurance suggestions
                        
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

        Map<String, Object> response = new HashMap<>();
        response.put("agent", "risk-agent");
        response.put("destination", destination);
        response.put("risk_category", riskCategory);

        try {
            // Generate comprehensive risk assessment based on destination and category
            Map<String, Object> assessment = generateRiskAssessment(destination, riskCategory);
            response.put("assessment", assessment);
            response.put("status", "success");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in RiskAssessmentAgent.assessRisk", e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }

        return response;
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
                Map<String, Object> riskResult = assessRisk(destination,
                    riskCategory != null ? riskCategory : "general");
                response.put("response", riskResult);
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
        // Comprehensive response logic with peak season and alternative timing information
        if (query.toLowerCase().contains("health")) {
            return """
                    HEALTH RISK ASSESSMENT:
                    I can provide information about:
                    - Required and recommended vaccinations
                    - Common diseases in the destination
                    - Medical facility quality and availability
                    - Health insurance coverage recommendations
                    - Altitude sickness and climate adaptation
                    
                    Which destination would you like health risk information for?
                    """;
        } else if (query.toLowerCase().contains("security") || query.toLowerCase().contains("safe")) {
            return """
                    SECURITY ASSESSMENT:
                    I can assess:
                    - Current security situation
                    - Safe neighborhoods and areas to avoid
                    - Local crime statistics
                    - Emergency procedures
                    - Travel advisory levels
                    
                    Which destination are you concerned about?
                    """;
        } else if (query.toLowerCase().contains("weather")) {
            return """
                    WEATHER RISK ASSESSMENT:
                    I can provide:
                    - Seasonal weather patterns
                    - Extreme weather risks (monsoons, hurricanes, blizzards)
                    - Best weather months for travel
                    - Weather-related safety precautions
                    
                    Which destination's weather do you want to assess?
                    """;
        } else if (query.toLowerCase().contains("peak") || query.toLowerCase().contains("season") || query.toLowerCase().contains("price") || query.toLowerCase().contains("cost")) {
            return """
                    PEAK SEASON & PRICING ANALYSIS:
                    I can analyze:
                    - Peak tourism seasons and crowds
                    - High vs. low season pricing differences
                    - Best times to visit for value (shoulder seasons)
                    - Holiday and festival impacts on prices
                    - Weather vs. price trade-offs
                    
                    Which destination would you like pricing and season information for?
                    """;
        } else if (query.toLowerCase().contains("insurance") || query.toLowerCase().contains("document")) {
            return """
                    TRAVEL INSURANCE & DOCUMENTS:
                    I can advise on:
                    - Travel insurance necessity and coverage
                    - Required documents and visas
                    - Passport validity requirements
                    - Travel registry recommendations
                    - Emergency evacuation insurance
                    
                    Which destination needs document/insurance information?
                    """;
        } else if (query.toLowerCase().contains("best") || query.toLowerCase().contains("when") || query.toLowerCase().contains("alternative")) {
            return """
                    BEST TIME TO VISIT ANALYSIS:
                    I can recommend:
                    - Best months for weather and activities
                    - Shoulder seasons for fewer crowds
                    - Off-season advantages and disadvantages
                    - Festival and holiday periods
                    - Seasonal activity availability
                    
                    Which destination are you planning to visit?
                    """;
        } else {
            return """
                    COMPREHENSIVE RISK ASSESSMENT:
                    I'm your travel safety advisor. I can help with:
                    - SAFETY: Security, health, emergency procedures
                    - SEASONS: Peak times, crowds, pricing, best times to visit
                    - WEATHER: Hazards, best months, climate adaptation
                    - DOCUMENTS: Visas, insurance, vaccination requirements
                    - COSTS: Price variations, alternative timing for savings
                    
                    What risk assessment would you like? (destination name and category)
                    """;
        }
    }

    /**
     * Generate a comprehensive risk assessment for a given destination and category
     */
    private static Map<String, Object> generateRiskAssessment(String destination, String riskCategory) {
        Map<String, Object> assessment = new HashMap<>();
        String destLower = destination.toLowerCase();

        // Location-specific risk assessments
        if (destLower.contains("raleigh") || destLower.contains("north carolina")) {
            assessment.put("safetyLevel", "High");
            assessment.put("peakSeasons", "Spring (March-May) and Fall (September-October)");
            assessment.put("bestTimesToVisit", "April-May, September-October");
            assessment.put("healthRisks", "Standard US health risks, good medical facilities available");
            assessment.put("weatherHazards", "Occasional winter storms, summer thunderstorms");
            assessment.put("recommendations", "Standard travel precautions, winter tires if visiting in winter");
        } else if (destLower.contains("paris")) {
            assessment.put("safetyLevel", "High");
            assessment.put("peakSeasons", "Summer (June-August)");
            assessment.put("bestTimesToVisit", "April-May, September-October");
            assessment.put("healthRisks", "Excellent medical facilities, standard European health risks");
            assessment.put("weatherHazards", "Occasional summer heatwaves, winter cold");
            assessment.put("recommendations", "Be aware of pickpocketing in tourist areas, good healthcare system");
        } else if (destLower.contains("tokyo")) {
            assessment.put("safetyLevel", "Very High");
            assessment.put("peakSeasons", "Cherry blossom season (March-April), Autumn foliage (October-November)");
            assessment.put("bestTimesToVisit", "April-May, September-October");
            assessment.put("healthRisks", "World-class medical facilities, low disease risk");
            assessment.put("weatherHazards", "Typhoon season (June-October), occasional earthquakes");
            assessment.put("recommendations", "Excellent safety record, prepare for typhoon season");
        } else if (destLower.contains("bangkok")) {
            assessment.put("safetyLevel", "Moderate");
            assessment.put("peakSeasons", "Cool season (November-February)");
            assessment.put("bestTimesToVisit", "November-February");
            assessment.put("healthRisks", "Dengue fever, good medical facilities in tourist areas");
            assessment.put("weatherHazards", "Heavy monsoon rains (May-October), flooding");
            assessment.put("recommendations", "Use mosquito repellent, avoid street food if concerned, good hospitals available");
        } else if (destLower.contains("bali")) {
            assessment.put("safetyLevel", "Moderate");
            assessment.put("peakSeasons", "Dry season (April-October)");
            assessment.put("bestTimesToVisit", "April-October");
            assessment.put("healthRisks", "Dengue fever, rabies in some areas, good tourist medical facilities");
            assessment.put("weatherHazards", "Wet season rains (October-March), occasional volcanic activity");
            assessment.put("recommendations", "Get travel insurance, use mosquito repellent, avoid rabies areas");
        } else if (destLower.contains("london")) {
            assessment.put("safetyLevel", "High");
            assessment.put("peakSeasons", "Summer (June-August)");
            assessment.put("bestTimesToVisit", "April-May, September-October");
            assessment.put("healthRisks", "Excellent NHS system, standard European health risks");
            assessment.put("weatherHazards", "Variable weather, occasional fog, winter cold");
            assessment.put("recommendations", "Be aware of pickpocketing, excellent healthcare available");
        } else if (destLower.contains("new york") || destLower.contains("nyc")) {
            assessment.put("safetyLevel", "High");
            assessment.put("peakSeasons", "Summer (June-August)");
            assessment.put("bestTimesToVisit", "April-May, September-October");
            assessment.put("healthRisks", "World-class medical facilities, standard urban health risks");
            assessment.put("weatherHazards", "Winter snowstorms, summer heatwaves, occasional hurricanes");
            assessment.put("recommendations", "Standard urban precautions, good emergency services");
        } else if (destLower.contains("miami")) {
            assessment.put("safetyLevel", "Moderate");
            assessment.put("peakSeasons", "Winter (December-March)");
            assessment.put("bestTimesToVisit", "April-May, October-November");
            assessment.put("healthRisks", "Good medical facilities, occasional heat-related issues");
            assessment.put("weatherHazards", "Hurricane season (June-November), summer thunderstorms");
            assessment.put("recommendations", "Hurricane preparedness, stay hydrated in summer");
        } else if (destLower.contains("los angeles") || destLower.contains("la")) {
            assessment.put("safetyLevel", "Moderate");
            assessment.put("peakSeasons", "Summer (June-August)");
            assessment.put("bestTimesToVisit", "April-May, September-October");
            assessment.put("healthRisks", "Excellent medical facilities, occasional wildfires");
            assessment.put("weatherHazards", "Earthquake risk, wildfires, occasional flooding");
            assessment.put("recommendations", "Earthquake preparedness, check air quality during fire season");
        } else if (destLower.contains("chicago")) {
            assessment.put("safetyLevel", "Moderate");
            assessment.put("peakSeasons", "Summer (June-August)");
            assessment.put("bestTimesToVisit", "May, September-October");
            assessment.put("healthRisks", "Good medical facilities, winter cold exposure");
            assessment.put("weatherHazards", "Severe winter storms, summer heatwaves");
            assessment.put("recommendations", "Winter preparedness, stay cool in summer");
        } else if (destLower.contains("san francisco") || destLower.contains("sf")) {
            assessment.put("safetyLevel", "High");
            assessment.put("peakSeasons", "Summer (June-August)");
            assessment.put("bestTimesToVisit", "April-May, September-October");
            assessment.put("healthRisks", "Excellent medical facilities, occasional fog-related issues");
            assessment.put("weatherHazards", "Earthquake risk, dense fog, wildfires");
            assessment.put("recommendations", "Earthquake preparedness, check air quality");
        } else {
            // Default assessment for unknown destinations
            assessment.put("safetyLevel", "Moderate");
            assessment.put("peakSeasons", "Varies by location");
            assessment.put("bestTimesToVisit", "Shoulder seasons when possible");
            assessment.put("healthRisks", "Check local health advisories, ensure travel insurance");
            assessment.put("weatherHazards", "Check local weather patterns and seasonal risks");
            assessment.put("recommendations", "Research local conditions, get comprehensive travel insurance");
        }

        // Add category-specific details
        if ("health".equals(riskCategory)) {
            assessment.put("vaccinations", "Check CDC recommendations for required vaccinations");
            assessment.put("medicalFacilities", "Research hospital quality and locations");
            assessment.put("insurance", "Travel health insurance strongly recommended");
        } else if ("security".equals(riskCategory)) {
            assessment.put("crimeRate", "Check local crime statistics");
            assessment.put("safeAreas", "Research safe neighborhoods and areas to avoid");
            assessment.put("emergencyNumbers", "Save local emergency contact numbers");
        } else if ("weather".equals(riskCategory)) {
            assessment.put("seasonalPatterns", "Research local climate patterns");
            assessment.put("extremeWeather", "Check for hurricanes, typhoons, or severe storms");
            assessment.put("clothing", "Pack appropriate clothing for weather conditions");
        }

        return assessment;
    }
}
