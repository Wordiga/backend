package com.wordiga.tourism.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TourismContentFestivalListResponse {

    private List<TourismContentFestivalDto> items;
    private int page;
    private int size;
    private Integer totalCount;
    private Integer totalPages;
    private boolean hasNext;
}
