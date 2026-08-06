package com.wordiga.service;

import com.wordiga.dto.tourismContent.detail.MonthlyWeatherDto;
import com.wordiga.global.client.WeatherApiClient;
import com.wordiga.global.client.dto.AsosDailyResponse;
import com.wordiga.global.config.WeatherProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonthlyWeatherService {
    private static final Map<String, String> STATIONS = Map.ofEntries(
            Map.entry("110", "232"), Map.entry("120", "232"), Map.entry("150", "236"),
            Map.entry("180", "235"), Map.entry("200", "232"), Map.entry("210", "129"),
            Map.entry("230", "236"), Map.entry("250", "236"), Map.entry("270", "129"),
            Map.entry("310", "238"), Map.entry("330", "236"), Map.entry("340", "235"),
            Map.entry("350", "236"), Map.entry("360", "129"), Map.entry("370", "129"),
            Map.entry("380", "129"));

    private final WeatherApiClient weatherApiClient;
    private final WeatherProperties properties;

    public MonthlyWeatherDto estimate(String sigunguCode, LocalDate visitDate) {
        String stationId = STATIONS.get(sigunguCode);
        if (stationId == null) return null;
        int years = properties.historicalYears() <= 0 ? 5 : properties.historicalYears();
        List<AsosDailyResponse.Item> observations = new ArrayList<>();
        for (int offset = 1; offset <= years; offset++) {
            YearMonth target = YearMonth.of(LocalDate.now().getYear() - offset, visitDate.getMonthValue());
            observations.addAll(weatherApiClient.daily(stationId, target.atDay(1), target.atEndOfMonth()));
        }
        List<BigDecimal> temperatures = observations.stream().map(AsosDailyResponse.Item::getAvgTa)
                .map(this::number).filter(java.util.Objects::nonNull).toList();
        if (observations.isEmpty()) return null;
        BigDecimal precipitation = observations.stream().map(AsosDailyResponse.Item::getSumRn)
                .map(this::number).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal temperature = temperatures.isEmpty() ? null : temperatures.stream()
                                                                 .reduce(BigDecimal.ZERO, BigDecimal::add)
                                                                 .divide(BigDecimal.valueOf(temperatures.size()), 1, RoundingMode.HALF_UP);
        return MonthlyWeatherDto.builder().targetMonth(visitDate.getMonthValue())
                .estimatedAverageTemperatureCelsius(temperature)
                .estimatedMonthlyPrecipitationMillimeters(
                        precipitation.divide(BigDecimal.valueOf(years), 1, RoundingMode.HALF_UP))
                .historicalYears(years).stationName(observations.getFirst().getStnNm())
                .basis("ASOS_HISTORICAL_MONTHLY_AVERAGE").build();
    }

    private BigDecimal number(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
