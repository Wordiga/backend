package com.wordiga.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "weather.api")
@EnableConfigurationProperties(WeatherProperties.class)
public record WeatherProperties(String baseUrl, String serviceKey, int historicalYears) {
}