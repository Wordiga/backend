package com.wordiga.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsDailyResponse {
    private Response response;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private List<Item> item;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private AwsMonthDays awsmdays;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AwsMonthDays {
        @JsonProperty("stn_id")
        private String stationId;
        @JsonProperty("stn_ko")
        private String stationName;
        private List<DailyObservation> info;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyObservation {
        private String tm;
        @JsonProperty("ta_min")
        private String minTemperature;
        @JsonProperty("ta_max")
        private String maxTemperature;
        @JsonProperty("rn_day")
        private String dailyPrecipitation;
    }
}
