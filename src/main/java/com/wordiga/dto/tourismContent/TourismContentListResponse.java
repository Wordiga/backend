package com.wordiga.dto.tourismContent;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TourismContentListResponse {

    private List<TourismContentDto> items;
    private int page;
    private int size;
    private boolean hasNext;
}
