package com.wordiga.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai.server")
@EnableConfigurationProperties(AiServerProperties.class)
public record AiServerProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}