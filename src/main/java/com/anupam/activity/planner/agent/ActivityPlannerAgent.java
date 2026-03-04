package com.anupam.activity.planner.agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;

import com.anupam.common.events.ActivityQueryEvent;
import com.anupam.common.events.ActivityResponseEvent;
import com.anupam.common.eventbus.EventBusProvider;
import com.google.common.eventbus.Subscribe;
import com.anupam.orchestrator.registry.AgentRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ActivityPlannerAgent - Handles activity recommendations and planning
 */
public class ActivityPlannerAgent {

    private static final Logger LOGGER = Logger.getLogger(ActivityPlannerAgent.class.getName());

    public static final BaseAgent ROOT_AGENT = initAgent();

    private static BaseAgent initAgent() {
        return LlmAgent.builder()
                .name("activity-agent")
                .description("Recommends activities and helps plan activities for travel destinations")
                .instruction("""
                        You are an activity planning assistant for the intelligent travel platform.
                        You help users with:
                        - Recommending activities based on interests
                        - Planning daily activity schedules
                        - Suggesting indoor/outdoor activities by weather
                        - Finding activities suitable for specific times of day
                        - Providing activity booking information
                        
                        Use the available tools to provide personalized activity recommendations.
                        """)
                .model("gemini-2.5-flash")
                .tools(FunctionTool.create(ActivityPlannerAgent.class, "recommendActivities"))
                .build();
    }

    // Register as EventBus subscriber
    static {
        EventBusProvider.getEventBus().register(new ActivityPlannerAgent());
    }

    @Subscribe
    public void onActivityQuery(ActivityQueryEvent event) {
        if (event == null) return;
        String city = event.getCity();
        String corrId = event.getCorrelationId();
        try {
            LOGGER.info(() -> "ActivityPlannerAgent.onActivityQuery called for city=" + city + " corrId=" + corrId);
            // Try to fetch travel and weather via AgentRegistry (fallback synchronous calls)
            Map<String, Object> travelData = new HashMap<>();
            Map<String, Object> weatherData = new HashMap<>();
            try {
                Map<String, Object> travelResp = AgentRegistry.routeToAgent("travel-agent", "Plan a trip to " + city);
                if (travelResp != null && travelResp.containsKey("response") && travelResp.get("response") instanceof Map) {
                    travelData = (Map<String, Object>) travelResp.get("response");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "ActivityPlannerAgent: failed to fetch travel agent response", ex);
            }

            try {
                Map<String, Object> weatherResp = AgentRegistry.routeToAgent("weather-agent", "What's the weather in " + city);
                if (weatherResp != null && weatherResp.containsKey("response") && weatherResp.get("response") instanceof Map) {
                    weatherData = (Map<String, Object>) weatherResp.get("response");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "ActivityPlannerAgent: failed to fetch weather agent response", ex);
            }

            Map<String, Object> activityResponse = handleCoordinatedQuery(city, travelData, weatherData);
            Map<String, Object> responseData = new HashMap<>();
            if (activityResponse != null && activityResponse.containsKey("response") && activityResponse.get("response") instanceof Map) {
                responseData = (Map<String, Object>) activityResponse.get("response");
            }

            // Publish the activity response event
            EventBusProvider.getEventBus().post(new ActivityResponseEvent(corrId, responseData));
            LOGGER.info(() -> "ActivityPlannerAgent posted ActivityResponseEvent for corrId=" + corrId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "ActivityPlannerAgent: error processing ActivityQueryEvent for corrId=" + corrId, e);
            EventBusProvider.getEventBus().post(new ActivityResponseEvent(corrId, new HashMap<>(), "error"));
        }
    }

    @Schema(description = "Recommend activities for a destination")
    public static Map<String, Object> recommendActivities(
            @Schema(name = "destination", description = "The destination city") String destination,
            @Schema(name = "interests", description = "User's interests (e.g., adventure, culture, food)") String interests) {

        return ActivityPlannerAgent.handleQuery("Recommend " + interests + " activities in " + destination);
    }

    /**
     * Handle activity-related queries
     */
    public static Map<String, Object> handleQuery(String query) {
        Map<String, Object> response = new HashMap<>();
        response.put("agent", "activity-agent");
        response.put("query", query);

        // Extract destination and interests from the query
        String destination = extractDestination(query);
        String interests = extractInterests(query);

        if (destination != null && !destination.isEmpty()) {
            response.put("response", recommendActivities(destination,
                interests != null ? interests : "general").get("response"));
        } else {
            response.put("response", generateActivityResponse(query));
        }

        return response;
    }

    /**
     * Handle coordinated activity recommendations with travel and weather data
     * This method is called when ActivityAgent coordinates with TravelAgent and WeatherAgent
     */
    public static Map<String, Object> handleCoordinatedQuery(
            String city,
            Map<String, Object> travelData,
            Map<String, Object> weatherData) {

        Map<String, Object> response = new HashMap<>();
        response.put("agent", "activity-agent");
        response.put("city", city);
        response.put("coordination", true);
        response.put("output_key", "coordinated_activities");

        try {
            // Debug: Log what we received
            LOGGER.info(() -> "ActivityPlannerAgent.handleCoordinatedQuery called");
            LOGGER.info(() -> "  City: " + city);
            LOGGER.info(() -> "  Travel Data type: " + (travelData != null ? travelData.getClass().getSimpleName() : "null"));
            LOGGER.info(() -> "  Travel Data content: " + (travelData != null ? travelData : "null"));
            LOGGER.info(() -> "  Weather Data type: " + (weatherData != null ? weatherData.getClass().getSimpleName() : "null"));
            LOGGER.info(() -> "  Weather Data content: " + (weatherData != null ? weatherData : "null"));

            // Combine all data for intelligent recommendations
            Map<String, Object> coordinatedActivities = new HashMap<>();

            // ADD ACTUAL WEATHER AND TRAVEL DATA TO RESPONSE
            if (weatherData != null && !weatherData.isEmpty()) {
                coordinatedActivities.put("weather_information", weatherData);
            } else {
                coordinatedActivities.put("weather_information", "No weather data available");
            }

            if (travelData != null && !travelData.isEmpty()) {
                coordinatedActivities.put("travel_information", travelData);
            } else {
                coordinatedActivities.put("travel_information", "No travel data available");
            }

            // Add weather-based recommendations
            Map<String, Object> weatherBasedActivities = generateWeatherBasedActivities(city, weatherData);
            coordinatedActivities.put("weather_based_activities", weatherBasedActivities);

            // Add travel-based recommendations
            Map<String, Object> travelBasedActivities = generateTravelBasedActivities(city, travelData);
            coordinatedActivities.put("travel_based_activities", travelBasedActivities);

            // Add general top activities
            Map<String, Object> topActivities = generateTopActivities(city);
            coordinatedActivities.put("top_activities", topActivities);

            response.put("response", coordinatedActivities);
            response.put("status", "success");

            LOGGER.info("ActivityPlannerAgent response prepared successfully");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "ERROR in handleCoordinatedQuery: " + e.getMessage(), e);
            response.put("status", "error");
            response.put("error", e.getMessage());
            response.put("response", new HashMap<>());
        }

        return response;
    }

    /**
     * Generate activities based on weather conditions
     */
    private static Map<String, Object> generateWeatherBasedActivities(String city, Map<String, Object> weatherData) {
        Map<String, Object> activities = new HashMap<>();
        activities.put("city", city);
        activities.put("based_on", "weather");

        if (weatherData != null && !weatherData.isEmpty()) {
            // Check if weatherData has "text" (wrapped string) or "condition" (normal structure)
            Object condition = weatherData.get("condition");
            if (condition == null) {
                condition = weatherData.get("text");
            }
            Object tempObj = weatherData.get("temperature");

            if (condition != null) {
                String weatherCondition = condition.toString().toLowerCase();

                System.out.println("Weather condition detected: " + weatherCondition);

                if (weatherCondition.contains("sunny") || weatherCondition.contains("clear")) {
                    activities.put("recommendation", "Perfect weather for outdoor activities!");
                    activities.put("outdoor_activities", new String[]{
                        "Beach visit",
                        "City walking tour",
                        "Outdoor hiking",
                        "Picnic in park",
                        "Water sports"
                    });
                } else if (weatherCondition.contains("snow") || weatherCondition.contains("ice")) {
                    activities.put("recommendation", "Cold/snowy weather - perfect for indoor activities!");
                    activities.put("indoor_activities", new String[]{
                        "Museum of Fine Arts",
                        "New England Aquarium",
                        "Shopping in Back Bay",
                        "Theater shows",
                        "Indoor dining",
                        "Library visits",
                        "Art galleries"
                    });
                } else if (weatherCondition.contains("rain") || weatherCondition.contains("cloudy")) {
                    activities.put("recommendation", "Rainy/cloudy weather - indoor activities recommended!");
                    activities.put("indoor_activities", new String[]{
                        "Museum visit",
                        "Art gallery",
                        "Shopping mall",
                        "Movie theater",
                        "Indoor climbing",
                        "Cooking class"
                    });
                } else {
                    activities.put("recommendation", "Variable weather - flexible activities available!");
                    activities.put("flexible_activities", new String[]{
                        "Local markets",
                        "Cafes and restaurants",
                        "Cultural sites",
                        "Shopping"
                    });
                }
            } else {
                System.out.println("No condition found in weather data. Keys: " + weatherData.keySet());
            }
        } else {
            activities.put("recommendation", "Weather data not available - default activities");
            activities.put("default_activities", new String[]{
                "Local attractions",
                "Parks and gardens",
                "Museums",
                "Shopping",
                "Dining"
            });
        }

        return activities;
    }

    /**
     * Generate activities based on travel itinerary
     */
    private static Map<String, Object> generateTravelBasedActivities(String city, Map<String, Object> travelData) {
        Map<String, Object> activities = new HashMap<>();
        activities.put("city", city);
        activities.put("based_on", "travel_plan");

        if (travelData != null) {
            // Extract duration if available
            Object duration = travelData.get("duration");
            if (duration != null) {
                activities.put("duration_recommendation", "For " + duration + " days, we recommend:");
                if (duration.toString().matches("\\d+")) {
                    int days = Integer.parseInt(duration.toString());
                    if (days <= 2) {
                        activities.put("recommended_activities", new String[]{
                            "Must-see attractions",
                            "Local cuisine tasting",
                            "Shopping in main areas"
                        });
                    } else if (days <= 5) {
                        activities.put("recommended_activities", new String[]{
                            "Deep city exploration",
                            "Multiple cultural sites",
                            "Local day trips",
                            "Nightlife experiences"
                        });
                    } else {
                        activities.put("recommended_activities", new String[]{
                            "Complete city tour",
                            "Extended day trips",
                            "Local workshops",
                            "Alternative neighborhoods",
                            "Adventure activities"
                        });
                    }
                }
            }
        }

        return activities;
    }

    /**
     * Generate top activities for a city
     */
    private static Map<String, Object> generateTopActivities(String city) {
        Map<String, Object> activities = new HashMap<>();
        activities.put("city", city);
        activities.put("category", "top_rated");

        Map<String, String[]> cityActivities = new HashMap<>();
        cityActivities.put("paris", new String[]{
            "Eiffel Tower visit",
            "Louvre Museum",
            "Notre-Dame Cathedral",
            "Seine River cruise",
            "French cuisine dining"
        });
        cityActivities.put("tokyo", new String[]{
            "Senso-ji Temple",
            "Shibuya Crossing",
            "Robot Restaurant show",
            "Sushi making class",
            "Mount Fuji day trip"
        });
        cityActivities.put("new york", new String[]{
            "Statue of Liberty",
            "Central Park tour",
            "Broadway show",
            "Times Square",
            "Museum of Natural History"
        });
        cityActivities.put("barcelona", new String[]{
            "Sagrada Familia",
            "Park Güell",
            "Gothic Quarter walk",
            "Beach time",
            "Tapas tasting"
        });

        String[] topActivitiesForCity = cityActivities.getOrDefault(city.toLowerCase(), new String[]{
            "Local museum visit",
            "City walking tour",
            "Local restaurant dining",
            "Shopping at local markets",
            "Photography tour"
        });

        activities.put("activities", topActivitiesForCity);
        return activities;
    }

    /**
     * Extract destination from query
     */
    private static String extractDestination(String query) {
        String lowerQuery = query.toLowerCase();

        String[] patterns = {
            "in ",
            "at ",
            "for "
        };

        for (String pattern : patterns) {
            int index = lowerQuery.indexOf(pattern);
            if (index != -1) {
                String remaining = query.substring(index + pattern.length()).trim();
                String destination = remaining.split("[?!.,;]")[0].trim();
                if (!destination.isEmpty() && !destination.equals("me")) {
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
     * Extract interests from query
     */
    private static String extractInterests(String query) {
        String lowerQuery = query.toLowerCase();

        // Common activity keywords
        String[] keywords = {
            "adventure", "culture", "museum", "food", "hiking", "beach",
            "water sports", "nightlife", "shopping", "nature", "historical",
            "spiritual", "outdoor", "indoor", "relaxation"
        };

        for (String keyword : keywords) {
            if (lowerQuery.contains(keyword)) {
                return keyword;
            }
        }

        return null;
    }

    private static String generateActivityResponse(String query) {
        // Simple response logic - can be expanded with actual activity API integration
        if (query.toLowerCase().contains("recommend")) {
            return "I can recommend amazing activities! Tell me your destination and interests (adventure, culture, food, nature, etc.)";
        } else if (query.toLowerCase().contains("adventure")) {
            return "Great! For adventure activities, I can suggest hiking, rock climbing, water sports, and more. Which destination are you visiting?";
        } else if (query.toLowerCase().contains("culture") || query.toLowerCase().contains("museum")) {
            return "Cultural activities are wonderful! I can suggest museums, historical sites, local tours, and cultural experiences.";
        } else if (query.toLowerCase().contains("food")) {
            return "Food lover! I can recommend local restaurants, food tours, cooking classes, and authentic dining experiences.";
        }
        return "I'm your activity planning assistant. What activities interest you?";
    }
}

