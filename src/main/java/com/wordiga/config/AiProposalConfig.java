package com.wordiga.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties({AiServerProperties.class, ProposalS3Properties.class})
public class AiProposalConfig {
    @Bean S3Client s3Client(ProposalS3Properties p) { return S3Client.builder().region(Region.of(p.region())).build(); }
    @Bean S3Presigner s3Presigner(ProposalS3Properties p) { return S3Presigner.builder().region(Region.of(p.region())).build(); }
}
