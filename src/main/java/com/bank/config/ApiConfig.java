package com.bank.config;

public class ApiConfig {
    public static final String BASE_URL;
    public static final boolean ENABLED;

    static {
        String baseUrl = System.getenv("BANK_API_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = System.getProperty("bank.api.base.url", "").trim();
        }
        BASE_URL = baseUrl == null ? "" : baseUrl;
        ENABLED = !BASE_URL.isBlank();
    }
}
