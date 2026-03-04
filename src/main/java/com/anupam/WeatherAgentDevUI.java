package com.anupam;

import com.google.adk.web.AdkWebServer;
import com.anupam.orchestrator.agent.OrchestratorAgent;

public class WeatherAgentDevUI {

    public static void main(String[] args) {
        AdkWebServer.start(OrchestratorAgent.ROOT_AGENT);
    }
}

