package com.wordiga.tourism.service;

import com.wordiga.tourism.dto.SatisfactionRequestDto;
import com.wordiga.global.client.TourismApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TourismSatisfactionServiceUnitExceptionTest {

    @Mock
    private TourismApiClient tourismApiClient;

    @InjectMocks
    private TourismSatisfactionService service;

    @Test
    void returnsNullWhenEveryPublicDataSourceIsMissing() {
        SatisfactionRequestDto request = new SatisfactionRequestDto(
                "44", "200", "현충사", null,
                java.util.Map.of("invalid", java.math.BigDecimal.ONE), null);

        assertThat(service.calculate(request, new TourismSatisfactionService.CalculationCache()))
                .isNull();
    }

    @Test
    void acceptsNullCacheAndUsesOneNightStayCode() {
        SatisfactionRequestDto request = new SatisfactionRequestDto(
                "44", "200", "현충사", null, java.util.Map.of(), 1);

        assertThat(service.calculate(request, null)).isNull();

        verify(tourismApiClient).fetchStayIntensity(
                anyString(), eq("44"), eq("44200"), eq("2103"));
    }

    @Test
    void mapsTwoAndThreeNightStayCodes() {
        service.calculate(new SatisfactionRequestDto(
                "44", "200", "현충사", null, java.util.Map.of(), 2), null);
        service.calculate(new SatisfactionRequestDto(
                "44", "200", "현충사", null, java.util.Map.of(), 3), null);

        verify(tourismApiClient).fetchStayIntensity(
                anyString(), eq("44"), eq("44200"), eq("2104"));
        verify(tourismApiClient).fetchStayIntensity(
                anyString(), eq("44"), eq("44200"), eq("2105"));
    }
}
