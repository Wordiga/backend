package com.wordiga.tourism.dto.detail;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MonthlyWeatherDto {
    private Integer targetMonth;                // 대상 월 (1~12)
    private BigDecimal avgTemp;                 // 월평균 기온 (℃)
    private BigDecimal monthlyPrecipitation;    // 월평균 누적 강수량 (mm)
    private Integer historicalYears;            // 참조 과거 년수 (예: 5년)
    private String stationName;                 // 참조 ASOS 관측소명
    private String basis;                       // 데이터 산출 근거 식별자
}
