package com.wordiga.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static com.fasterxml.jackson.databind.DeserializationFeature.*;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .enable(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .build();
    }
}