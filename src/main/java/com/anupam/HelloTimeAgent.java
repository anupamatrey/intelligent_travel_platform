package com.anupam;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;


public class HelloTimeAgent {

    private static final Logger LOGGER = Logger.getLogger(HelloTimeAgent.class.getName());

    public static BaseAgent ROOT_AGENT = initAgent();

    private static BaseAgent initAgent() {
        return LlmAgent.builder()
                .name("hello-time-agent")
                .description("Tells the current time in a specified city")
                .instruction("""
                        You are a helpful assistant that tells the current time in a city.
                        Use the 'getCurrentTime' tool for this purpose.
                        """)
                .model("gemini-2.5-flash")
                .tools(FunctionTool.create(HelloTimeAgent.class, "getCurrentTime"))
                .build();
    }

    /**
     * Mock tool implementation
     */
    @Schema(description = "Get the current time for a given city")
    public static Map<String, String> getCurrentTime(
            @Schema(name = "city", description = "Name of the city to get the time for") String city) {
        try {
            LOGGER.info(() -> "HelloTimeAgent.getCurrentTime called for city=" + city);
            return Map.of(
                    "city", city,
                    "forecast", "The time is 10:30am."
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "HelloTimeAgent.getCurrentTime failed for city=" + city, e);
            return Map.of(
                    "city", city,
                    "forecast", "error"
            );
        }
    }
}
