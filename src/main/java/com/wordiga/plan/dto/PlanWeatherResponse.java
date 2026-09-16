package com.wordiga.plan.dto;

import com.wordiga.tourism.dto.detail.MonthlyWeatherDto;

import java.math.BigDecimal;

public record PlanWeatherResponse(
        String locationName,
        Integer targetMonth,
        BigDecimal averageMinTemp,
        BigDecimal averageMaxTemp,
        Integer averageRainyDays,
        BigDecimal monthlyPrecipitation,
        Integer historicalYears,
        String stationName,
        String description) {

    public static PlanWeatherResponse from(String locationName, MonthlyWeatherDto weather) {
        return new PlanWeatherResponse(locationName, weather.getTargetMonth(), weather.getAverageMinTemp(),
                weather.getAverageMaxTemp(), weather.getAverageRainyDays(),
                weather.getMonthlyPrecipitation(), weather.getHistoricalYears(), weather.getStationName(),
                weather.getDescription());
    }
}
