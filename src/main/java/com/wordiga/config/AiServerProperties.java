package com.wordiga.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("ai.server")
public record AiServerProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) { }
