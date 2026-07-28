package com.wordiga.dto.tourismContent.detail;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SpendingIndexDto {

    private String regionName;
    private String categoryName;
    private BigDecimal indexValue;
    private String referencePeriod;
}
