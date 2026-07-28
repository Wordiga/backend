package com.wordiga.dto.tourismContent.detail;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class SeasonalImageDto {

    private String imageUrl;
    private String thumbnailUrl;
    private LocalDate shootingDate;
    private Season season;
    private BigDecimal matchConfidence;

    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER, UNKNOWN
    }
}
