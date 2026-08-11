package com.wordiga.tourism.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum TourismContentType {
    // TODO: 한국관광공사 승인 필요 여부 확인
    TOURIST_ATTRACTION("12", "관광지"),
    CULTURAL_FACILITY("14", "문화시설"),
    FESTIVAL("15", "행사/공연/축제"),
    TRAVEL_COURSE("25", "여행코스"),
    LEPORTS("28", "레포츠"),
    LODGING("32", "숙박"),
    SHOPPING("38", "쇼핑"),
    RESTAURANT("39", "음식점"),
    OTHER(null, "기타");

    private final String code;
    private final String displayName;

    public static TourismContentType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code != null && type.code.equals(code))
                .findFirst()
                .orElse(OTHER);
    }
}
