package com.wordiga.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "proposal.s3")
@EnableConfigurationProperties(ProposalS3Properties.class)
public record ProposalS3Properties(String bucket, String region, Duration urlValidity, Duration retention) {
}