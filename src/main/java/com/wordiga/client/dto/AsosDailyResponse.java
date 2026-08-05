package com.wordiga.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
public class AsosDailyResponse {
    private Response response;
    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response { private Header header; private Body body; }
    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header { private String resultCode; private String resultMsg; }
    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body { private Items items; }
    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items { private List<Item> item; }
    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String tm; private String stnId; private String stnNm;
        private String avgTa; private String sumRn;
    }
}
