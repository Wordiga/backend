package com.wordiga.tourism.service;

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
    void averagesThreeYearsOfSameMonthAsExpectedWeather() {
        AsosDailyResponse.Item first = item("아산", "15.0", "25.0", "10.0");
        AsosDailyResponse.Item second = item("아산", "17.0", "27.0", "0.0");
        when(weatherApiClient.daily(eq("232"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(first, second));
        MonthlyWeatherService service = new MonthlyWeatherService(weatherApiClient,
                new WeatherProperties("https://weather", "key", 3));

        var result = service.estimate("133", LocalDate.of(2026, 9, 10));

        assertThat(result.getTargetMonth()).isEqualTo(9);
        assertThat(result.getAverageMinTemp()).isEqualByComparingTo("16.0");
        assertThat(result.getAverageMaxTemp()).isEqualByComparingTo("26.0");
        assertThat(result.getAverageRainyDays()).isEqualTo(1);
        assertThat(result.getMonthlyPrecipitation()).isEqualByComparingTo("10.0");
        assertThat(result.getHistoricalYears()).isEqualTo(3);
        assertThat(result.getStationName()).isEqualTo("천안");
        assertThat(result.getBasis()).isEqualTo("ASOS_HISTORICAL_MONTHLY_AVERAGE");
        verify(weatherApiClient, times(3)).daily(eq("232"), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void returnsNullWhenSigunguHasNoStationMapping() {
        MonthlyWeatherService service = new MonthlyWeatherService(weatherApiClient,
                new WeatherProperties("https://weather", "key", 5));

        assertThat(service.estimate("999", LocalDate.of(2026, 9, 10))).isNull();
        verifyNoInteractions(weatherApiClient);
    }

    private AsosDailyResponse.Item item(String station, String min, String max, String precipitation) {
        AsosDailyResponse.Item item = new AsosDailyResponse.Item();
        item.setStnNm(station);
        item.setMinTa(min);
        item.setMaxTa(max);
        item.setSumRn(precipitation);
        return item;
    }
}
