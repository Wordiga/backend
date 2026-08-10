package com.wordiga.tourism.dto.detail;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
public class SatisfactionDto {

    private BigDecimal totalScore;
    private ScoreComponentDto popularityScore;
    private ScoreComponentDto ageFitScore;
    private ScoreComponentDto stayFitScore;
    private ScoreComponentDto comfortScore;
    private OffsetDateTime calculatedAt;
}
