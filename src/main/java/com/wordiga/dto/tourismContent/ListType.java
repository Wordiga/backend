package com.wordiga.dto.tourismContent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ListType {
    POPULAR("인기순"),
    SEASONAL("시즌 추천"),
    PERSONALIZED("위시 기반 개인화 추천"),
    RELATED("연관 관광콘텐츠 추천");

    private final String description;
}
