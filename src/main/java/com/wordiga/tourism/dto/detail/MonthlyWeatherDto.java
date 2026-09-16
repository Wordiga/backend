package com.wordiga.tourism.dto.detail;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MonthlyWeatherDto {
    private Integer targetMonth;                // 대상 월 (1~12)
    private BigDecimal averageMinTemp;          // 일 최저기온 월평균 (℃)
    private BigDecimal averageMaxTemp;          // 일 최고기온 월평균 (℃)
    private Integer averageRainyDays;           // 월평균 강수일수 (0.1mm 이상)
    private BigDecimal monthlyPrecipitation;    // 월평균 누적 강수량 (mm)
    private Integer historicalYears;            // 참조 과거 년수
    private String stationName;                 // 참조 관측소명
    private String description;                 // 데이터 산출 기준 설명
}
