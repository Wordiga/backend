package com.wordiga.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("proposal.s3")
public record ProposalS3Properties(String bucket, String region, Duration urlValidity, Duration retention) { }
