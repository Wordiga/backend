package com.wordiga.service;

import com.wordiga.global.client.WeatherApiClient;
import com.wordiga.global.client.dto.AsosDailyResponse;
import com.wordiga.global.config.WeatherProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyWeatherServiceUnitTest {
    @Mock
    WeatherApiClient weatherApiClient;

    @Test
    void averagesFiveYearsOfSameMonthAsExpectedWeather() {
        AsosDailyResponse.Item first = item("아산", "20.0", "10.0");
        AsosDailyResponse.Item second = item("아산", "22.0", "20.0");
        when(weatherApiClient.daily(eq("232"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(first, second));
        MonthlyWeatherService service = new MonthlyWeatherService(weatherApiClient,
                new WeatherProperties("https://weather", "key", 5));

        var result = service.estimate("110", LocalDate.of(2026, 9, 10));

        assertThat(result.getTargetMonth()).isEqualTo(9);
        assertThat(result.getEstimatedAverageTemperatureCelsius()).isEqualByComparingTo("21.0");
        assertThat(result.getEstimatedMonthlyPrecipitationMillimeters()).isEqualByComparingTo("30.0");
        assertThat(result.getHistoricalYears()).isEqualTo(5);
        assertThat(result.getBasis()).isEqualTo("ASOS_HISTORICAL_MONTHLY_AVERAGE");
        verify(weatherApiClient, times(5)).daily(eq("232"), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void returnsNullWhenSigunguHasNoStationMapping() {
        MonthlyWeatherService service = new MonthlyWeatherService(weatherApiClient,
                new WeatherProperties("https://weather", "key", 5));

        assertThat(service.estimate("999", LocalDate.of(2026, 9, 10))).isNull();
        verifyNoInteractions(weatherApiClient);
    }

    private AsosDailyResponse.Item item(String station, String temperature, String precipitation) {
        AsosDailyResponse.Item item = new AsosDailyResponse.Item();
        item.setStnNm(station);
        item.setAvgTa(temperature);
        item.setSumRn(precipitation);
        return item;
    }
}
