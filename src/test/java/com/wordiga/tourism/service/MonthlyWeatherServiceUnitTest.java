package com.wordiga.tourism.service;

import com.wordiga.global.client.WeatherApiClient;
import com.wordiga.global.client.dto.AwsDailyResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
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
    void calculatesPreviousYearMonthFromOneAwsCall() {
        AwsDailyResponse.DailyObservation first = item("15.0", "25.0", "10.0");
        AwsDailyResponse.DailyObservation second = item("17.0", "27.0", "0.0");
        when(weatherApiClient.daily(eq("634"), anyInt(), eq(9)))
                .thenReturn(List.of(first, second));
        MonthlyWeatherService service = new MonthlyWeatherService(weatherApiClient);

        var result = service.estimate("200", LocalDate.of(2026, 9, 10));

        assertThat(result.getTargetMonth()).isEqualTo(9);
        assertThat(result.getAverageMinTemp()).isEqualByComparingTo("16.0");
        assertThat(result.getAverageMaxTemp()).isEqualByComparingTo("26.0");
        assertThat(result.getAverageRainyDays()).isEqualTo(1);
        assertThat(result.getMonthlyPrecipitation()).isEqualByComparingTo("10.0");
        assertThat(result.getHistoricalYears()).isEqualTo(1);
        assertThat(result.getStationName()).isEqualTo("아산");
        assertThat(result.getDescription()).isEqualTo("직전 연도 동일 월 관측값");
        verify(weatherApiClient).daily(eq("634"), eq(LocalDate.now().getYear() - 1), eq(9));
    }

    @Test
    void mapsAllSupportedTourismSigunguCodesToTheirLocalStation() {
        Map<String, String> stations = Map.ofEntries(
                Map.entry("130", "232"), Map.entry("131", "232"), Map.entry("133", "617"),
                Map.entry("150", "612"), Map.entry("180", "235"), Map.entry("200", "634"),
                Map.entry("210", "129"), Map.entry("230", "615"), Map.entry("250", "636"),
                Map.entry("270", "616"), Map.entry("710", "238"), Map.entry("760", "236"),
                Map.entry("770", "614"), Map.entry("790", "618"), Map.entry("800", "177"),
                Map.entry("810", "628"), Map.entry("825", "627"));
        when(weatherApiClient.daily(anyString(), anyInt(), eq(9)))
                .thenReturn(List.of(item("10", "20", "0")));
        MonthlyWeatherService service = new MonthlyWeatherService(weatherApiClient);

        stations.forEach((sigunguCode, stationId) -> {
            service.estimate(sigunguCode, 9);
            verify(weatherApiClient).daily(stationId, LocalDate.now().getYear() - 1, 9);
            clearInvocations(weatherApiClient);
        });
    }

    @Test
    void returnsNullWhenSigunguHasNoStationMapping() {
        MonthlyWeatherService service = new MonthlyWeatherService(weatherApiClient);

        assertThat(service.estimate("999", LocalDate.of(2026, 9, 10))).isNull();
        verifyNoInteractions(weatherApiClient);
    }

    private AwsDailyResponse.DailyObservation item(String min, String max, String precipitation) {
        AwsDailyResponse.DailyObservation item = new AwsDailyResponse.DailyObservation();
        item.setMinTemperature(min);
        item.setMaxTemperature(max);
        item.setDailyPrecipitation(precipitation);
        return item;
    }
}
