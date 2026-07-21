package com.wordiga.dto.tourismContent;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TourismSeasonalListResponse {
    private String baseYm;
    private List<SeasonalItem> items;

    @Getter
    @Builder
    public static class SeasonalItem {
        private String areaCd;
        private String areaNm;
        private String signguCd;
        private String signguNm;
        private String indexName;
        private double indexValue;
    }
}
