package com.anupam;

import com.anupam.orchestrator.agent.OrchestratorAgent;

import java.util.Map;

public class TestEventBusRunner {
    public static void main(String[] args) {
        System.out.println("Starting EventBus smoke test: coordinateAgents(\"Boston\")");
        Map<String, Object> res = OrchestratorAgent.coordinateAgents("Boston");
        System.out.println("Result: " + res);
    }
}

