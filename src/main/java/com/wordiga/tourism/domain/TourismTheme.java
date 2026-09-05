package com.wordiga.tourism.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TourismTheme {
    ATTRACTION_EXPERIENCE("ATTRACTION_EXPERIENCE", "관광지/체험"),
    LODGING("LODGING", "숙소"),
    FOOD_CAFE("FOOD_CAFE", "맛집/카페");

    private final String code;
    private final String displayName;
}
