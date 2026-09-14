package com.wordiga.tourism.dto.detail;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TourismContentDetailResponse {

    private TourismCommonDetailDto common;
    private TourismIntroDetailDto intro;
    private List<TourismDetailInfoDto> details;
    private List<TourismDetailImageDto> images;
    private List<SeasonalImageDto> seasonalImages;
    private SatisfactionDto satisfaction;
    private Boolean capacitySatisfied;

    @JsonProperty("isWished")
    @Getter(onMethod_ = @JsonProperty("isWished"))
    private boolean isWished;
}
