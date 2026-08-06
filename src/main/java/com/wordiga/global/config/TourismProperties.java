package com.wordiga.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tourism")
public class TourismProperties {

    private Region region = new Region();
    private Api api = new Api();

    @Getter
    @Setter
    public static class Region {
        private String chungnamCode;
        private String chungnamName;
    }

    @Getter
    @Setter
    public static class Api {
        private String baseUrl;
        private String serviceKey;
        private String mobileOs;
        private String mobileApp;
        private int defaultNumOfRows;
    }
}