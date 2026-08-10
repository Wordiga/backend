package com.wordiga.tourism.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TourismPopularListResponse {
    private String baseYm;
    private List<PopularItem> items;

    @Getter
    @Builder
    public static class PopularItem {
        private String signguCd;
        private String signguNm;
        private double popularityScore;
        private double expenditureScore;
        private double stayScore;
    }
}
