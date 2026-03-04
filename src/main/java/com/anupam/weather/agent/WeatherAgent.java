package com.anupam.weather.agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.anupam.service.weather.WeatherService;

import com.anupam.common.events.ActivityQueryEvent;
import com.anupam.common.events.WeatherResponseEvent;
import com.anupam.common.eventbus.EventBusProvider;
import com.google.common.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WeatherAgent {

    private static final Logger LOGGER = Logger.getLogger(WeatherAgent.class.getName());

    public static final BaseAgent ROOT_AGENT = initAgent();

    private static BaseAgent initAgent() {
        return LlmAgent.builder()
                .name("weather-agent")
                .description("Provides current weather for a specified city")
                .instruction("""
                        You are a helpful assistant that returns current weather for a city.
                        Use the 'getWeather' tool for this purpose.
                        """)
                .model("gemini-2.5-flash")
                .tools(FunctionTool.create(WeatherAgent.class, "getWeather"))
                .build();
    }

    // Register as EventBus subscriber
    static {
        EventBusProvider.getEventBus().register(new WeatherAgent());
    }

    @Subscribe
    public void onActivityQuery(ActivityQueryEvent event) {
        if (event == null) return;
        String city = event.getCity();
        String corrId = event.getCorrelationId();
        try {
            LOGGER.info(() -> "[WeatherAgent] Received ActivityQueryEvent for city=" + city + " corrId=" + corrId);
            Map<String, Object> weatherData = getWeather(city);
            Map<String, Object> responseData = new HashMap<>();
            if (weatherData != null) {
                responseData.putAll(weatherData);
            }
            EventBusProvider.getEventBus().post(new WeatherResponseEvent(corrId, responseData));
            LOGGER.info(() -> "[WeatherAgent] Posted WeatherResponseEvent for corrId=" + corrId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[WeatherAgent] Error processing ActivityQueryEvent for corrId=" + corrId, e);
            EventBusProvider.getEventBus().post(new WeatherResponseEvent(corrId, new HashMap<>(), "error"));
        }
    }

    @Schema(description = "Get current weather for a given city")
    public static Map<String, Object> getWeather(
            @Schema(name = "city", description = "Name of the city to get the weather for") String city) {
        WeatherService svc = new WeatherService();
        try {
            Map<String, Object> w = svc.getWeather(city);
            LOGGER.info(() -> "[WeatherAgent] getWeather(" + city + ") returned keys=" + (w != null ? w.keySet() : "null"));
            return w;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[WeatherAgent] getWeather failed for city=" + city, e);
            return new HashMap<>();
        }
    }

    /**
     * Handle weather-related queries
     * This method is called by AgentRegistry when routing requests to this agent
     */
    public static Map<String, Object> handleQuery(String query) {
        Map<String, Object> response = new HashMap<>();
        response.put("agent", "weather-agent");
        response.put("query", query);

        // Extract city name from the query
        String city = extractCityName(query);

        if (city != null && !city.isEmpty()) {
            // Call the actual weather service
            Map<String, Object> weatherData = getWeather(city);
            response.put("response", weatherData);
        } else {
            response.put("response", generateWeatherResponse(query));
        }

        return response;
    }

    /**
     * Extract city name from query using simple pattern matching
     * Can be enhanced with NLP later
     */
    private static String extractCityName(String query) {
        String lowerQuery = query.toLowerCase();

        // Common patterns: "weather in [city]", "what's the weather in [city]", "[city] weather"
        String[] patterns = {
            "weather in ",
            "weather for ",
            "temperature in ",
            "forecast for ",
            "in "
        };

        for (String pattern : patterns) {
            int index = lowerQuery.indexOf(pattern);
            if (index != -1) {
                // Extract text after the pattern
                String remaining = query.substring(index + pattern.length()).trim();
                // Take the first word or until punctuation
                String city = remaining.split("[?!.,;]")[0].trim();
                if (!city.isEmpty()) {
                    return city;
                }
            }
        }

        // If no pattern matched, try to extract capitalized words (likely city names)
        String[] words = query.split("\\s+");
        for (String word : words) {
            if (word.length() > 2 && Character.isUpperCase(word.charAt(0))) {
                return word.replaceAll("[?!.,;]", "").trim();
            }
        }

        return null;
    }

    private static String generateWeatherResponse(String query) {
        // Simple response logic - can be expanded with actual weather API integration
        if (query.toLowerCase().contains("weather")) {
            return "I can help you get weather information. Please specify the city you'd like the weather for (e.g., 'What's the weather in Boston?').";
        } else if (query.toLowerCase().contains("forecast")) {
            return "I can provide weather forecasts. Which city and how many days ahead?";
        } else if (query.toLowerCase().contains("temperature") || query.toLowerCase().contains("rain")) {
            return "I can tell you about temperature and precipitation. Which location are you asking about?";
        }
        return "I'm your weather assistant. Ask me about weather, temperature, or forecasts for any city.";
    }
}
