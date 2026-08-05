package com.wordiga.dto.tourismContent.detail;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder
public class MonthlyWeatherDto {
    private Integer targetMonth;
    private BigDecimal estimatedAverageTemperatureCelsius;
    private BigDecimal estimatedMonthlyPrecipitationMillimeters;
    private Integer historicalYears;
    private String stationName;
    private String basis;
}
