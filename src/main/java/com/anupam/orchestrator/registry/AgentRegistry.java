package com.anupam.orchestrator.registry;

import com.anupam.weather.agent.WeatherAgent;
import com.anupam.travel.agent.TravelAgent;
import com.anupam.activity.planner.agent.ActivityPlannerAgent;
import com.anupam.risk.agent.RiskAssessmentAgent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * AgentRegistry - Central registry for all available agents
 *
 * This registry maintains:
 * 1. A mapping of agent names to their handler functions
 * 2. Methods to route requests to appropriate agents
 * 3. Agent discovery and management
 */
public class AgentRegistry {

    // Map of agent names to their request handlers
    private static final Map<String, Function<String, Map<String, Object>>> agentHandlers = new HashMap<>();

    static {
        // Register all available agents
        registerAgent("weather-agent", WeatherAgent::handleQuery);
        registerAgent("travel-agent", TravelAgent::handleQuery);
        registerAgent("activity-agent", ActivityPlannerAgent::handleQuery);
        registerAgent("risk-agent", RiskAssessmentAgent::handleQuery);
    }

    /**
     * Register an agent with the registry
     *
     * @param agentName The name of the agent
     * @param handler A function that processes queries for this agent
     */
    public static void registerAgent(String agentName, Function<String, Map<String, Object>> handler) {
        agentHandlers.put(agentName, handler);
    }

    /**
     * Route a request to the appropriate agent
     *
     * @param agentName The name of the target agent
     * @param query The user's query
     * @return The response from the agent
     */
    public static Map<String, Object> routeToAgent(String agentName, String query) {
        Function<String, Map<String, Object>> handler = agentHandlers.get(agentName.toLowerCase());

        if (handler == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Agent not found: " + agentName);
            errorResponse.put("available_agents", agentHandlers.keySet());
            return errorResponse;
        }

        try {
            return handler.apply(query);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing request with agent: " + agentName);
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Get list of available agents
     *
     * @return Set of available agent names
     */
    public static java.util.Set<String> getAvailableAgents() {
        return agentHandlers.keySet();
    }

    /**
     * Check if an agent is registered
     *
     * @param agentName The name of the agent
     * @return true if agent is registered, false otherwise
     */
    public static boolean isAgentRegistered(String agentName) {
        return agentHandlers.containsKey(agentName.toLowerCase());
    }
}

