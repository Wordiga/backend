package com.wordiga.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "weather.api")
@EnableConfigurationProperties(WeatherProperties.class)
@Validated
public record WeatherProperties(String baseUrl,
                                String serviceKey) {
}
