package com.wordiga.tourism.controller;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.plan.dto.PlanWeatherResponse;
import com.wordiga.tourism.service.MonthlyWeatherService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Validated
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {
    private final MonthlyWeatherService monthlyWeatherService;
    private final TourismApiClient tourismApiClient;

    @GetMapping
    public ResponseEntity<PlanWeatherResponse> getWeather(
            @RequestParam String lDongSignguCd,
            @RequestParam @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "방문 월은 YYYY-MM 형식이어야 합니다.")
            String visitMonth) {
        int month;
        try {
            month = YearMonth.parse(visitMonth).getMonthValue();
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방문 월은 YYYY-MM 형식이어야 합니다.");
        }
        var weather = monthlyWeatherService.estimate(lDongSignguCd, month);
        if (weather == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "지역의 날씨를 조회할 수 없습니다.");
        String locationName = tourismApiClient.fetchSigunguList().stream()
                .filter(item -> lDongSignguCd.equals(item.getCode()))
                .map(com.wordiga.global.client.dto.SigunguItem::getName)
                .findFirst().orElse(null);
        return ResponseEntity.ok(PlanWeatherResponse.from(locationName, weather));
    }
}
