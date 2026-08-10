package com.wordiga.tourism.dto.detail;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomImageDto {

    private String imageUrl;
    private String alt;
    private String copyrightTypeCode;
}
