package com.wordiga.tourism.dto.detail;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ScoreComponentDto {

    private BigDecimal score;
    private BigDecimal weight;
    private boolean imputed;
    private String reason;
    private String source;
}
