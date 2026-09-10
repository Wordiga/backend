package com.wordiga.plan.dto;

import com.wordiga.tourism.dto.detail.MonthlyWeatherDto;

import java.math.BigDecimal;

public record PlanWeatherResponse(
        String locationName,
        Integer targetMonth,
        BigDecimal avgTemp,
        BigDecimal monthlyPrecipitation,
        Integer historicalYears,
        String stationName,
        String basis) {

    public static PlanWeatherResponse from(String locationName, MonthlyWeatherDto weather) {
        return new PlanWeatherResponse(locationName, weather.getTargetMonth(), weather.getAvgTemp(),
                weather.getMonthlyPrecipitation(), weather.getHistoricalYears(), weather.getStationName(),
                weather.getBasis());
    }
}
