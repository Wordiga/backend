package com.wordiga.tourism.dto.detail;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TourismDetailImageDto {

    private String imageName;
    private String imageUrl;
    private String thumbnailUrl;
    private String copyrightTypeCode;
    private Integer serialNumber;
}
