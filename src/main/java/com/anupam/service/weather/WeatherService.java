package com.anupam.service.weather;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public class WeatherService {
    private static final String API_URL = "http://api.weatherapi.com/v1/current.json";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns a map representing the weather API response or an error map.
     * Success: returns the parsed JSON as a Map (contains "current" and "location").
     * Error: returns a map with keys: status, city, weather
     */
    public Map<String, Object> getWeather(String city) {
        if (city == null || city.isBlank()) {
            return Map.of(
                    "status", "error",
                    "city", city,
                    "weather", "city must be provided"
            );
        }

        String apiKey = System.getenv("WEATHER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of(
                    "status", "error",
                    "city", city,
                    "weather", "WEATHER_API_KEY not set in environment."
            );
        }

        try {
            String q = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String key = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String uri = API_URL + "?key=" + key + "&q=" + q;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();

            if (status >= 200 && status < 300) {
                JsonNode root = mapper.readTree(body);
                if (root.has("current") && root.has("location")) {
                    // convert to Map and return
                    Map<String, Object> data = mapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
                    return data;
                } else if (root.has("error")) {
                    JsonNode err = root.get("error");
                    String msg = err.has("message") ? err.get("message").asText() : "Unknown error.";
                    return Map.of(
                            "status", "error",
                            "city", city,
                            "weather", msg
                    );
                } else {
                    return Map.of(
                            "status", "error",
                            "city", city,
                            "weather", "Unexpected API response"
                    );
                }
            } else {
                return Map.of(
                        "status", "error",
                        "city", city,
                        "weather", "HTTP " + status + ": " + Objects.toString(body, "" )
                );
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return Map.of(
                    "status", "error",
                    "city", city,
                    "weather", "Request interrupted: " + ie.getMessage()
            );
        } catch (IOException ioe) {
            return Map.of(
                    "status", "error",
                    "city", city,
                    "weather", "IO error: " + ioe.getMessage()
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "city", city,
                    "weather", Objects.toString(e.getMessage(), e.toString())
            );
        }
    }
}

