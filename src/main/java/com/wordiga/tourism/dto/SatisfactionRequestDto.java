package com.wordiga.tourism.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SatisfactionRequestDto(
        String areaCode,
        String localSignguCode,
        String title,
        LocalDate visitDate,
        Map<String, BigDecimal> ageGroupRatios, // 예: {"20": 0.6, "30": 0.4} (합=1.0)
        Integer stayNights                       // null(미지정), 0(당일), 1(1박), 2(2박), 3(3박 이상)
) {
}
