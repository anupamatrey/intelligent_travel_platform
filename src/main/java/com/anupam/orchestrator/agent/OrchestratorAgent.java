package com.anupam.orchestrator.agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.anupam.orchestrator.registry.AgentRegistry;

import com.anupam.common.events.ActivityQueryEvent;
import com.anupam.common.events.TravelResponseEvent;
import com.anupam.common.events.WeatherResponseEvent;
import com.anupam.common.messages.CorrelationIdGenerator;
import com.anupam.common.eventbus.EventBusProvider;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OrchestratorAgent - Routes user requests to appropriate specialized agents
 *
 * The orchestrator acts as a dispatcher/router that:
 * 1. Receives user input
 * 2. Determines which specialized agent should handle the request
 * 3. Routes the request to the appropriate agent
 * 4. Supports multi-agent coordination with parallel execution
 */
public class OrchestratorAgent {

    private static final Logger LOGGER = Logger.getLogger(OrchestratorAgent.class.getName());

    public static final BaseAgent ROOT_AGENT = initAgent();

    private static BaseAgent initAgent() {
        return LlmAgent.builder()
                .name("orchestrator-agent")
                .description("Routes user requests to appropriate specialized agents (weather, travel, activity planning, risk assessment)")
                .instruction("""
                        You are an intelligent orchestrator agent for a travel platform.
                        Your job is to analyze user requests and route them to the most appropriate agent or coordinator.
                        
                        IMPORTANT: When you receive coordinated responses from multiple agents, ALWAYS include ALL the data in your response:
                        - Weather information (temperature, conditions, recommendations)
                        - Travel information (itinerary, duration, logistics)
                        - Risk assessment (safety, health, peak seasons, pricing, alternatives)
                        - Activity recommendations (based on all factors)
                        
                        DO NOT filter out or summarize away the weather, travel, or risk data.
                        Always present the COMPLETE structured response including all these sections.
                        
                        For activity-related queries that mention a city or location, ALWAYS use coordinateAgents()
                        because you need to combine travel, weather, AND risk assessment information to provide the best activity recommendations.
                        
                        Available options:
                        1. For ACTIVITY queries (e.g., "activities in Boston", "what to do in Paris", "plan activities for 3 days in Boston"):
                           → Use 'coordinateAgents' function with the city name
                           → This will call Travel Agent, Weather Agent, AND Risk Assessment Agent in parallel
                           → Returns comprehensive activity recommendations with risk warnings and alternative travel times
                           → INCLUDE all weather, travel, and risk data in your response to the user
                        
                        2. For single domain queries:
                           - weather-agent: "What's the weather in Paris?" or "Temperature in London?"
                           - travel-agent: "Plan my trip to Rome" or "Create itinerary for Tokyo"
                           - risk-agent: "Is Thailand safe?" or "Health precautions for India?" or "Best time to visit Mexico?"
                        
                        DECISION LOGIC:
                        - If query is about "activities", "things to do", "what to do", "plan activities" → Use coordinateAgents()
                        - If query mentions multiple aspects (travel + activities + conditions) → Use coordinateAgents()
                        - Otherwise, analyze and call 'routeRequest' with the most appropriate single agent.
                        
                        The coordinateAgents function now includes:
                        - Travel information (itineraries, duration, logistics)
                        - Weather conditions (current and forecasted)
                        - Risk assessment (safety, health, peak season pricing, best times to visit)
                        - Activity recommendations based on all three factors
                        
                        When returning the response, ensure the user sees:
                        1. Weather information clearly stated
                        2. Travel planning details
                        3. Risk assessment including peak seasons and pricing
                        4. Personalized activity recommendations
                        5. Health and safety guidance
                        
                        Always be helpful and include ALL available information. Do not omit data sections.
                        """)
                .model("gemini-2.5-flash")
                .tools(
                    FunctionTool.create(OrchestratorAgent.class, "routeRequest"),
                    FunctionTool.create(OrchestratorAgent.class, "coordinateAgents")
                )
                .build();
    }

    @Schema(description = "Route a user request to a single specialized agent")
    public static Map<String, Object> routeRequest(
            @Schema(name = "agentName", description = "The name of the target agent (weather-agent, travel-agent, activity-agent, or risk-agent)") String agentName,
            @Schema(name = "query", description = "The user's query to be processed by the selected agent") String query) {

        return AgentRegistry.routeToAgent(agentName, query);
    }

    // Simple response holders keyed by correlation id
    private static final ConcurrentMap<String, CompletableFuture<Map<String, Object>>> travelResponseFutures = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, CompletableFuture<Map<String, Object>>> weatherResponseFutures = new ConcurrentHashMap<>();

    // Register this class as subscriber to EventBus on class load
    static {
        EventBusProvider.getEventBus().register(new OrchestratorAgent());
    }

    @Subscribe
    public void handleTravelResponse(TravelResponseEvent event) {
        if (event == null || event.getCorrelationId() == null) return;
        String corrId = event.getCorrelationId();
        CompletableFuture<Map<String, Object>> future = travelResponseFutures.get(corrId);
        if (future != null) {
            future.complete(event.getData());
        }
    }

    @Subscribe
    public void handleWeatherResponse(WeatherResponseEvent event) {
        if (event == null || event.getCorrelationId() == null) return;
        String corrId = event.getCorrelationId();
        CompletableFuture<Map<String, Object>> future = weatherResponseFutures.get(corrId);
        if (future != null) {
            future.complete(event.getData());
        }
    }

    @Schema(description = "Coordinate multiple agents in parallel - Activity agent calls Travel and Weather agents")
    public static Map<String, Object> coordinateAgents(
            @Schema(name = "city", description = "The city for which to coordinate agents") String city) {

        Map<String, Object> coordinatedResponse = new HashMap<>();
        coordinatedResponse.put("orchestrator", "orchestrator-agent");
        coordinatedResponse.put("city", city);
        coordinatedResponse.put("coordination_type", "activity-travel-weather");

        try {
            LOGGER.info(() -> "[OrchestratorAgent] coordinateAgents called for city: " + city);

            // Use EventBus to publish ActivityQueryEvent and wait for responses
            String corrId = CorrelationIdGenerator.generateCorrelationId();

            // Prepare futures and register
            CompletableFuture<Map<String, Object>> travelFuture = new CompletableFuture<>();
            CompletableFuture<Map<String, Object>> weatherFuture = new CompletableFuture<>();
            travelResponseFutures.put(corrId, travelFuture);
            weatherResponseFutures.put(corrId, weatherFuture);

            EventBus eventBus = EventBusProvider.getEventBus();
            // Publish the query event - TravelAgent and WeatherAgent will listen
            eventBus.post(new ActivityQueryEvent(city, corrId));

            // Wait for both with timeout
            Map<String, Object> travelData = null;
            Map<String, Object> weatherData = null;
            try {
                travelData = travelFuture.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException te) {
                LOGGER.log(Level.WARNING, "[OrchestratorAgent] Travel agent timeout/error for corrId=" + corrId);
            }

            try {
                weatherData = weatherFuture.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException te) {
                LOGGER.log(Level.WARNING, "[OrchestratorAgent] Weather agent timeout/error for corrId=" + corrId);
            }

            // Cleanup futures
            travelResponseFutures.remove(corrId);
            weatherResponseFutures.remove(corrId);

            // If both are null fall back to previous behavior
            if (travelData == null && weatherData == null) {
                // Fallback to direct parallel calls
                LOGGER.info("[OrchestratorAgent] Fallback to direct agent calls");
                Map<String, Object> activityResponse = callActivityAgentWithCoordination(city);

                if (activityResponse != null && activityResponse.containsKey("response")) {
                    coordinatedResponse.put("response", activityResponse.get("response"));
                } else {
                    coordinatedResponse.put("response", activityResponse);
                }

                coordinatedResponse.put("status", "success");
                return coordinatedResponse;
            }

            // Ensure maps exist
            if (travelData == null) travelData = new HashMap<>();
            if (weatherData == null) weatherData = new HashMap<>();

            // Fallback: create empty risk data if not available
            Map<String, Object> riskData = new HashMap<>();

            // Call ActivityPlannerAgent with collected data
            LOGGER.info("[OrchestratorAgent] Calling ActivityPlannerAgent.handleCoordinatedQuery");
            Map<String, Object> activityResponse = com.anupam.activity.planner.agent.ActivityPlannerAgent.handleCoordinatedQuery(city, travelData, weatherData, riskData);

            if (activityResponse != null && activityResponse.containsKey("response")) {
                coordinatedResponse.put("response", activityResponse.get("response"));
            } else {
                coordinatedResponse.put("response", activityResponse);
            }

            coordinatedResponse.put("status", "success");
            LOGGER.info("[OrchestratorAgent] Coordinated response ready");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[OrchestratorAgent] ERROR in coordinateAgents: " + e.getMessage(), e);
            coordinatedResponse.put("status", "error");
            coordinatedResponse.put("error", e.getMessage());
            coordinatedResponse.put("error_type", e.getClass().getSimpleName());
        }

        return coordinatedResponse;
    }

    /**
     * ActivityPlannerAgent calls TravelAgent, WeatherAgent, and RiskAssessmentAgent in parallel
     */
    public static Map<String, Object> callActivityAgentWithCoordination(String city) {
        Map<String, Object> combinedResponse = new HashMap<>();
        combinedResponse.put("orchestrator", "orchestrator-agent");
        combinedResponse.put("city", city);
        combinedResponse.put("coordination_type", "activity-travel-weather-risk");

        // Create thread pool for parallel execution (3 agents: travel, weather, risk)
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        try {
            LOGGER.info(() -> "[OrchestratorAgent] Starting parallel coordination for city: " + city);

            // Task 1: Call TravelAgent
            Future<Map<String, Object>> travelFuture = executorService.submit(() -> {
                try {
                    String travelQuery = "Plan a trip to " + city;
                    LOGGER.info(() -> "[OrchestratorAgent] Calling TravelAgent with query: " + travelQuery);
                    Map<String, Object> result = AgentRegistry.routeToAgent("travel-agent", travelQuery);
                    LOGGER.info(() -> "[OrchestratorAgent] TravelAgent returned: " +
                        (result != null ? result.keySet() : "null"));
                    return result;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "[OrchestratorAgent] ERROR in TravelAgent: " + e.getMessage(), e);
                    throw e;
                }
            });

            // Task 2: Call WeatherAgent
            Future<Map<String, Object>> weatherFuture = executorService.submit(() -> {
                try {
                    String weatherQuery = "What's the weather in " + city;
                    LOGGER.info(() -> "[OrchestratorAgent] Calling WeatherAgent with query: " + weatherQuery);
                    Map<String, Object> result = AgentRegistry.routeToAgent("weather-agent", weatherQuery);
                    LOGGER.info(() -> "[OrchestratorAgent] WeatherAgent returned: " +
                        (result != null ? result.keySet() : "null"));
                    return result;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "[OrchestratorAgent] ERROR in WeatherAgent: " + e.getMessage(), e);
                    throw e;
                }
            });

            // Task 3: Call RiskAssessmentAgent
            Future<Map<String, Object>> riskFuture = executorService.submit(() -> {
                try {
                    String riskQuery = "Assess travel risks for " + city + " including weather hazards, peak season costs, and alternative timing";
                    LOGGER.info(() -> "[OrchestratorAgent] Calling RiskAssessmentAgent with query: " + riskQuery);
                    Map<String, Object> result = AgentRegistry.routeToAgent("risk-agent", riskQuery);
                    LOGGER.info(() -> "[OrchestratorAgent] RiskAssessmentAgent returned: " +
                        (result != null ? result.keySet() : "null"));
                    return result;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "[OrchestratorAgent] ERROR in RiskAssessmentAgent: " + e.getMessage(), e);
                    throw e;
                }
            });

            // Wait for all three to complete with timeout
            LOGGER.info("[OrchestratorAgent] Waiting for all agents to complete (timeout: 10s)");
            Map<String, Object> travelResponse = travelFuture.get(10, TimeUnit.SECONDS);
            Map<String, Object> weatherResponse = weatherFuture.get(10, TimeUnit.SECONDS);
            Map<String, Object> riskResponse = riskFuture.get(10, TimeUnit.SECONDS);
            LOGGER.info("[OrchestratorAgent] All agents completed successfully");

            // Extract the actual response data from all agents
            // Handle both Map and String responses
            Object travelResponseObj = travelResponse.get("response");
            Object weatherResponseObj = weatherResponse.get("response");
            Object riskResponseObj = riskResponse.get("response");

            LOGGER.info(() -> "[OrchestratorAgent] Travel response type: " +
                (travelResponseObj != null ? travelResponseObj.getClass().getSimpleName() : "null"));
            LOGGER.info(() -> "[OrchestratorAgent] Weather response type: " +
                (weatherResponseObj != null ? weatherResponseObj.getClass().getSimpleName() : "null"));
            LOGGER.info(() -> "[OrchestratorAgent] Risk response type: " +
                (riskResponseObj != null ? riskResponseObj.getClass().getSimpleName() : "null"));

            // Convert to Map - keep the actual weather data structure
            Map<String, Object> travelData = (travelResponseObj instanceof Map) ? 
                (Map<String, Object>) travelResponseObj : new HashMap<>();
            
            Map<String, Object> weatherData = (weatherResponseObj instanceof Map) ? 
                (Map<String, Object>) weatherResponseObj : new HashMap<>();
            
            Map<String, Object> riskData = (riskResponseObj instanceof Map) ? 
                (Map<String, Object>) riskResponseObj : new HashMap<>();

            LOGGER.info(() -> "[OrchestratorAgent] Travel data: " + (travelData != null && !travelData.isEmpty() ? "present" : "empty"));
            LOGGER.info(() -> "[OrchestratorAgent] Weather data: " + (weatherData != null && !weatherData.isEmpty() ? "present" : "empty"));
            LOGGER.info(() -> "[OrchestratorAgent] Risk data: " + (riskData != null && !riskData.isEmpty() ? "present" : "empty"));

            // Call ActivityPlannerAgent.handleCoordinatedQuery directly with combined data
            LOGGER.info("[OrchestratorAgent] Calling ActivityPlannerAgent.handleCoordinatedQuery");
            Map<String, Object> activityResponse =
                com.anupam.activity.planner.agent.ActivityPlannerAgent.handleCoordinatedQuery(city, travelData, weatherData, riskData);

            LOGGER.info(() -> "[OrchestratorAgent] ActivityPlannerAgent returned: " +
                (activityResponse != null ? activityResponse.keySet() : "null"));

            combinedResponse.put("response", activityResponse.get("response"));
            combinedResponse.put("status", "success");
            LOGGER.info("[OrchestratorAgent] Coordination complete - returning combined response");

        } catch (TimeoutException e) {
            LOGGER.log(Level.SEVERE, "[OrchestratorAgent] TIMEOUT: One or more agents took too long", e);
            combinedResponse.put("status", "timeout");
            combinedResponse.put("error", "One or more agents took too long to respond");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[OrchestratorAgent] ERROR in callActivityAgentWithCoordination: " + e.getMessage(), e);
            combinedResponse.put("status", "error");
            combinedResponse.put("error", e.getMessage());
            combinedResponse.put("error_trace", getStackTrace(e));
        } finally {
            executorService.shutdown();
            LOGGER.info("[OrchestratorAgent] ExecutorService shutdown");
        }

        return combinedResponse;
    }

    /**
     * Helper method to get stack trace as string for debugging
     */
    private static String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}

