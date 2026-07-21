package com.wordiga.dto.tourismContent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ListType {
    POPULAR("인기순"),
    SEASONAL("시즌 추천");

    private final String description;
}