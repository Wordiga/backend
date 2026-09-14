package com.wordiga.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "storage.kt")
@EnableConfigurationProperties(KtStorageProperties.class)
public record KtStorageProperties(
        String bucket,
        String region,
        String endpoint,
        String accessKey,
        String secretKey,
        Duration urlValidity,
        Duration retention
) {
}