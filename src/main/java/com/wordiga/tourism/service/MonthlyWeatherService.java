package com.wordiga.tourism.service;

import com.wordiga.global.client.WeatherApiClient;
import com.wordiga.global.client.dto.AwsDailyResponse;
import com.wordiga.tourism.dto.detail.MonthlyWeatherDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.wordiga.global.util.KtoUtils.parseBigDecimal;

@Service
@RequiredArgsConstructor
public class MonthlyWeatherService {
    private static final Map<String, WeatherStation> STATION_BY_SIGUNGU_CODE = Map.ofEntries(
            Map.entry("130", new WeatherStation("232", "천안")),
            Map.entry("131", new WeatherStation("232", "천안")),
            Map.entry("133", new WeatherStation("617", "성거")),
            Map.entry("150", new WeatherStation("612", "공주")),
            Map.entry("180", new WeatherStation("235", "보령")),
            Map.entry("200", new WeatherStation("634", "아산")),
            Map.entry("210", new WeatherStation("129", "서산")),
            Map.entry("230", new WeatherStation("615", "논산")),
            Map.entry("250", new WeatherStation("636", "계룡")),
            Map.entry("270", new WeatherStation("616", "당진")),
            Map.entry("710", new WeatherStation("238", "금산")),
            Map.entry("760", new WeatherStation("236", "부여")),
            Map.entry("770", new WeatherStation("614", "서천")),
            Map.entry("790", new WeatherStation("618", "청양")),
            Map.entry("800", new WeatherStation("177", "홍성")),
            Map.entry("810", new WeatherStation("628", "예산")),
            Map.entry("825", new WeatherStation("627", "태안"))
    );

    private final WeatherApiClient weatherApiClient;

    /** 최근 완료된 연도의 같은 달 관측값으로 최저·최고기온, 강수일수, 강수량을 계산한다. */
    public MonthlyWeatherDto estimate(String sigunguCode, LocalDate visitDate) {
        return estimate(sigunguCode, visitDate.getMonthValue());
    }

    public MonthlyWeatherDto estimate(String sigunguCode, int month) {
        WeatherStation station = STATION_BY_SIGUNGU_CODE.get(sigunguCode);
        if (station == null) return null;

        int referenceYear = LocalDate.now().getYear() - 1;
        List<AwsDailyResponse.DailyObservation> observations =
                weatherApiClient.daily(station.id(), referenceYear, month);
        if (observations.isEmpty()) return null;

        // 1. 단일 순회 집계 (Single-pass Aggregation)
        BigDecimal totalMinTemp = BigDecimal.ZERO;
        BigDecimal totalMaxTemp = BigDecimal.ZERO;
        BigDecimal totalPrecipitation = BigDecimal.ZERO;
        long minTempCount = 0;
        long maxTempCount = 0;
        long rainyDays = 0;

        for (AwsDailyResponse.DailyObservation item : observations) {
            BigDecimal minTemp = parseBigDecimal(item.getMinTemperature());
            if (minTemp != null) {
                totalMinTemp = totalMinTemp.add(minTemp);
                minTempCount++;
            }
            BigDecimal maxTemp = parseBigDecimal(item.getMaxTemperature());
            if (maxTemp != null) {
                totalMaxTemp = totalMaxTemp.add(maxTemp);
                maxTempCount++;
            }

            BigDecimal rn = parseBigDecimal(item.getDailyPrecipitation());
            if (rn != null) {
                totalPrecipitation = totalPrecipitation.add(rn);
                if (rn.compareTo(BigDecimal.valueOf(0.1)) >= 0) rainyDays++;
            }
        }

        BigDecimal averageMinTemp = minTempCount == 0 ? null
                : totalMinTemp.divide(BigDecimal.valueOf(minTempCount), 1, RoundingMode.HALF_UP);
        BigDecimal averageMaxTemp = maxTempCount == 0 ? null
                : totalMaxTemp.divide(BigDecimal.valueOf(maxTempCount), 1, RoundingMode.HALF_UP);

        return MonthlyWeatherDto.builder()
                .targetMonth(month)
                .averageMinTemp(averageMinTemp)
                .averageMaxTemp(averageMaxTemp)
                .averageRainyDays(Math.toIntExact(rainyDays))
                .monthlyPrecipitation(totalPrecipitation.setScale(1, RoundingMode.HALF_UP))
                .historicalYears(1)
                .stationName(station.name())
                .description("직전 연도 동일 월 관측값")
                .build();
    }

    private record WeatherStation(String id, String name) {
    }
}
