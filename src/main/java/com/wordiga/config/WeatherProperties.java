package com.wordiga.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather.api")
public record WeatherProperties(String baseUrl, String serviceKey, int historicalYears) { }
