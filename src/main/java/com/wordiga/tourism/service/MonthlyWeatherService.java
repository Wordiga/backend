package com.wordiga.tourism.service;

import com.wordiga.global.client.WeatherApiClient;
import com.wordiga.global.client.dto.AsosDailyResponse;
import com.wordiga.global.config.WeatherProperties;
import com.wordiga.tourism.dto.detail.MonthlyWeatherDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.wordiga.global.util.KtoUtils.parseBigDecimal;

@Service
@RequiredArgsConstructor
public class MonthlyWeatherService {
    /**
     * 한국관광공사 법정동 시군구 코드에 대응하는 기상청 ASOS 관측소
     * 해당 시군구에 ASOS 관측소가 없으면 인접 지역의 대표 관측소를 사용
     */
    private static final Map<String, AsosStation> ASOS_STATION_BY_SIGUNGU_CODE = Map.ofEntries(
            Map.entry("110", new AsosStation("236", "부여")), // 공주시 -> 부여/대전 인접
            Map.entry("120", new AsosStation("238", "금산")), // 금산군 -> 금산 관측소
            Map.entry("131", new AsosStation("232", "천안")), // 천안시 동남구
            Map.entry("133", new AsosStation("232", "천안")), // 천안시 서북구
            Map.entry("150", new AsosStation("236", "부여")), // 논산시 -> 부여 인접
            Map.entry("180", new AsosStation("129", "서산")), // 당진시 -> 서산 인접
            Map.entry("200", new AsosStation("235", "보령")), // 보령시 -> 보령 관측소
            Map.entry("210", new AsosStation("236", "부여")), // 부여군 -> 부여 관측소
            Map.entry("230", new AsosStation("235", "보령")), // 서천군 -> 보령/군산 인접
            Map.entry("250", new AsosStation("232", "천안")), // 아산시 -> 천안 인접
            Map.entry("270", new AsosStation("129", "서산")), // 예산군 -> 서산/홍성 인접
            Map.entry("310", new AsosStation("232", "천안")), // 천안시 -> 천안 관측소
            Map.entry("330", new AsosStation("236", "부여")), // 청양군 -> 부여 인접
            Map.entry("340", new AsosStation("235", "보령")), // 태안군 -> 보령/서산 인접
            Map.entry("350", new AsosStation("129", "서산")), // 홍성군 -> 서산 인접
            Map.entry("360", new AsosStation("236", "부여")), // 계룡시 -> 부여/대전 인접
            Map.entry("370", new AsosStation("232", "천안")), // 세종시 -> 천안/대전 인접
            Map.entry("380", new AsosStation("129", "서산"))  // 당진시 등 -> 서산 인접
    );

    private final WeatherApiClient weatherApiClient;
    private final WeatherProperties properties;

    /**
     *  5개년 평균 기온/강수량 계산
     */
    public MonthlyWeatherDto estimate(String sigunguCode, LocalDate visitDate) {
        return estimate(sigunguCode, visitDate.getMonthValue());
    }

    public MonthlyWeatherDto estimate(String sigunguCode, int month) {
        AsosStation station = ASOS_STATION_BY_SIGUNGU_CODE.get(sigunguCode);
        if (station == null) return null;

        int years = properties.historicalYears();
        int currentYear = LocalDate.now().getYear();
        List<AsosDailyResponse.Item> observations = new ArrayList<>();

        for (int offset = 1; offset <= years; offset++) {
            YearMonth target = YearMonth.of(currentYear - offset, month);
            observations.addAll(weatherApiClient.daily(station.id(), target.atDay(1), target.atEndOfMonth()));
        }
        if (observations.isEmpty()) return null;

        // 1. 단일 순회 집계 (Single-pass Aggregation)
        BigDecimal totalTemp = BigDecimal.ZERO;
        BigDecimal totalPrecipitation = BigDecimal.ZERO;
        long tempCount = 0;

        for (AsosDailyResponse.Item item : observations) {
            BigDecimal temp = parseBigDecimal(item.getAvgTa());
            if (temp != null) {
                totalTemp = totalTemp.add(temp);
                tempCount++;
            }

            BigDecimal rn = parseBigDecimal(item.getSumRn());
            if (rn != null) {
                totalPrecipitation = totalPrecipitation.add(rn);
            }
        }

        // 2. 연산 수행 (평균 기온 및 과거 5년 기준 월평균 누적 강수량)
        BigDecimal avgTemp = tempCount == 0 ? null
                : totalTemp.divide(BigDecimal.valueOf(tempCount), 1, RoundingMode.HALF_UP);

        BigDecimal avgMonthlyPrecipitation = totalPrecipitation.divide(
                BigDecimal.valueOf(years), 1, RoundingMode.HALF_UP);

        return MonthlyWeatherDto.builder()
                .targetMonth(month)
                .avgTemp(avgTemp)
                .monthlyPrecipitation(avgMonthlyPrecipitation)
                .historicalYears(years)
                .stationName(station.name())
                .basis("ASOS_HISTORICAL_MONTHLY_AVERAGE")
                .build();
    }

    private record AsosStation(String id, String name) {
    }
}
