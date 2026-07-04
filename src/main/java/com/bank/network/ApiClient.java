package com.bank.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {
    private final HttpClient httpClient;
    private final String baseUrl;

    public record ApiResponse(int statusCode, String body) {
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public ApiResponse get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUri(path))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(response.statusCode(), response.body());
        } catch (Exception e) {
            return new ApiResponse(500, "ERROR: " + e.getMessage());
        }
    }

    public ApiResponse postJson(String path, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUri(path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(response.statusCode(), response.body());
        } catch (Exception e) {
            return new ApiResponse(500, "ERROR: " + e.getMessage());
        }
    }

    private URI buildUri(String path) {
        String trimmedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(baseUrl + trimmedPath);
    }
}
