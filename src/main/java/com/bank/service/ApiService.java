package com.bank.service;

import com.bank.config.ApiConfig;
import com.bank.network.ApiClient;

public class ApiService {
    private final boolean remoteEnabled = ApiConfig.ENABLED;
    private final ApiClient apiClient = remoteEnabled ? new ApiClient(ApiConfig.BASE_URL) : null;

    public boolean isRemoteEnabled() {
        return remoteEnabled;
    }

    public void notifyRemoteEvent(String eventType, String description) {
        if (!remoteEnabled || apiClient == null) {
            return;
        }

        String payload = "{"
                + "\"event\":\"" + escapeJson(eventType) + "\","
                + "\"description\":\"" + escapeJson(description) + "\""
                + "}";

        apiClient.postJson("/api/events", payload);
    }

    public boolean isApiReachable() {
        if (!remoteEnabled || apiClient == null) {
            return false;
        }

        var response = apiClient.get("/api/ping");
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
