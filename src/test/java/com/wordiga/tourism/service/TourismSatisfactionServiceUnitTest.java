package com.wordiga.tourism.service;

import com.wordiga.tourism.dto.SatisfactionRequestDto;
import com.wordiga.tourism.dto.detail.SatisfactionDto;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourismSatisfactionServiceUnitTest {

    @Mock
    private TourismApiClient tourismApiClient;

    @InjectMocks
    private TourismSatisfactionService service;

    @Test
    void calculatesWeightedSatisfaction() {
        mockPopularity();
        mockAgeFit();
        mockStayFit();
        mockComfort(LocalDate.of(2026, 8, 20));

        SatisfactionDto result = service.calculate(
                request("현충사", Map.of("20S", BigDecimal.ONE)), new TourismSatisfactionService.CalculationCache());

        assertThat(result.getPopularityScore().getScore()).isEqualByComparingTo("90.0");
        assertThat(result.getAgeFitScore().getScore()).isEqualByComparingTo("80.0");
        assertThat(result.getStayFitScore().getScore()).isEqualByComparingTo("70.0");
        assertThat(result.getComfortScore().getScore()).isEqualByComparingTo("80.0");
        assertThat(result.getTotalScore()).isEqualByComparingTo("81.0");
    }

    @Test
    void usesNeutralScoreWhenAgeInputIsMissing() {
        mockPopularity();
        mockStayFit();
        mockComfort(LocalDate.of(2026, 8, 20));

        SatisfactionDto result = service.calculate(
                request("현충사", Map.of()), new TourismSatisfactionService.CalculationCache());

        assertThat(result.getAgeFitScore().isImputed()).isTrue();
        assertThat(result.getAgeFitScore().getScore()).isEqualByComparingTo("50");
    }

    @Test
    void explainsWhenVisitDateIsOutsideConcentrationForecastRange() {
        mockPopularity();
        mockStayFit();
        mockComfort(LocalDate.of(2026, 8, 21));

        SatisfactionDto result = service.calculate(
                request("현충사", Map.of()), new TourismSatisfactionService.CalculationCache());

        assertThat(result.getComfortScore().isImputed()).isTrue();
        assertThat(result.getComfortScore().getReason()).contains("예측 제공 범위");
    }

    @Test
    void reusesRegionalSourcesWithinOneRecommendationRequest() {
        mockPopularity();
        mockStayFit();
        mockComfort(LocalDate.of(2026, 8, 20));
        when(tourismApiClient.fetchConcentrationRate("44", "44200", "외암민속마을"))
                .thenReturn(wrap(new TatsCnctrRateResponse(), List.of()));
        var cache = new TourismSatisfactionService.CalculationCache();

        service.calculate(request("현충사", Map.of()), cache);
        service.calculate(request("외암민속마을", Map.of()), cache);

        verify(tourismApiClient, times(1)).fetchCulturalResourceDemand(anyString(), eq("44"), eq("44200"), eq("12"));
        verify(tourismApiClient, times(1)).fetchServiceDemand(anyString(), eq("44"), eq("44200"), eq("11"));
        verify(tourismApiClient, times(1)).fetchStayIntensity(anyString(), eq("44"), eq("44200"), eq("2101"));
        verify(tourismApiClient, times(1)).fetchExpenditureIntensity(anyString(), eq("44"), eq("44200"), eq("2201"));
    }

    @Test
    void normalizesNullAgeGroupsAndReusesTheNeutralAgeResult() {
        mockPopularity();
        mockStayFit();
        mockComfort(LocalDate.of(2026, 8, 20));
        var cache = new TourismSatisfactionService.CalculationCache();

        SatisfactionDto nullAges = service.calculate(request("현충사", null), cache);
        SatisfactionDto agesContainingNull = service.calculate(request("현충사", Map.of()), cache);

        assertThat(nullAges.getAgeFitScore().isImputed()).isTrue();
        assertThat(agesContainingNull.getAgeFitScore().isImputed()).isTrue();
        verify(tourismApiClient, never()).fetchTouristDiversity(anyString(), anyString(), anyString(), anyString());
        verify(tourismApiClient, never()).fetchExpenditureDiversity(anyString(), anyString(), anyString(), anyString());
        verify(tourismApiClient, times(1)).fetchCulturalResourceDemand(anyString(), eq("44"), eq("44200"), eq("12"));
    }

    @Test
    void keepsAgeFitCacheSeparateForDifferentAgeGroups() {
        mockPopularity();
        mockAgeFit();
        mockStayFit();
        mockComfort(LocalDate.of(2026, 8, 20));
        var cache = new TourismSatisfactionService.CalculationCache();

        service.calculate(request("현충사", Map.of("20S", BigDecimal.ONE)), cache);
        service.calculate(request("현충사", Map.of("30S", BigDecimal.ONE)), cache);

        verify(tourismApiClient, times(2)).fetchTouristDiversity(anyString(), eq("44"), eq("44200"), anyString());
        verify(tourismApiClient, times(2)).fetchExpenditureDiversity(anyString(), eq("44"), eq("44200"), anyString());
        verify(tourismApiClient, times(1)).fetchCulturalResourceDemand(anyString(), eq("44"), eq("44200"), eq("12"));
    }

    private void mockPopularity() {
        AreaCulResDemItem resource = new AreaCulResDemItem();
        resource.setCulResDemIxVal("80");
        AreaCulResDemResponse resourceResponse = wrap(
                new AreaCulResDemResponse(), List.of(resource));
        when(tourismApiClient.fetchCulturalResourceDemand(
                anyString(), eq("44"), eq("44200"), eq("12")))
                .thenReturn(resourceResponse);

        AreaTarSvcDemItem serviceDemand = new AreaTarSvcDemItem();
        serviceDemand.setTarSvcDemIxVal("100");
        AreaTarSvcDemResponse serviceResponse = wrap(
                new AreaTarSvcDemResponse(), List.of(serviceDemand));
        when(tourismApiClient.fetchServiceDemand(
                anyString(), eq("44"), eq("44200"), eq("11")))
                .thenReturn(serviceResponse);
    }

    private void mockAgeFit() {
        AreaTouDivItem tourist = new AreaTouDivItem();
        tourist.setTouDivIxCd("3102");
        tourist.setTouDivIxVal("70");
        when(tourismApiClient.fetchTouristDiversity(
                anyString(), eq("44"), eq("44200"), anyString()))
                .thenReturn(wrap(new AreaTouDivResponse(), List.of(tourist)));

        AreaExpDivItem expenditure = new AreaExpDivItem();
        expenditure.setExpDivIxCd("3202");
        expenditure.setExpDivIxVal("90");
        when(tourismApiClient.fetchExpenditureDiversity(
                anyString(), eq("44"), eq("44200"), anyString()))
                .thenReturn(wrap(new AreaExpDivResponse(), List.of(expenditure)));
    }

    private void mockStayFit() {
        AreaTarSjrnDsItem stay = new AreaTarSjrnDsItem();
        stay.setTarSjrnDsIxVal("60");
        when(tourismApiClient.fetchStayIntensity(
                anyString(), eq("44"), eq("44200"), eq("2101")))
                .thenReturn(wrap(new AreaTarSjrnDsResponse(), List.of(stay)));

        AreaTarExpDsItem expenditure = new AreaTarExpDsItem();
        expenditure.setTarExpDsIxVal("80");
        when(tourismApiClient.fetchExpenditureIntensity(
                anyString(), eq("44"), eq("44200"), eq("2201")))
                .thenReturn(wrap(new AreaTarExpDsResponse(), List.of(expenditure)));
    }

    private void mockComfort(LocalDate visitDate) {
        TatsCnctrRateItem concentration = new TatsCnctrRateItem();
        concentration.setBaseYmd(visitDate.format(DateTimeFormatter.BASIC_ISO_DATE));
        concentration.setCnctrRate("20");
        when(tourismApiClient.fetchConcentrationRate("44", "44200", "현충사"))
                .thenReturn(wrap(new TatsCnctrRateResponse(), List.of(concentration)));
    }

    private SatisfactionRequestDto request(String title, Map<String, BigDecimal> ageRatios) {
        return new SatisfactionRequestDto(
                "44", "200", title, LocalDate.of(2026, 8, 20), ageRatios, 0);
    }

    private <T, R extends KtoApiResponse<T>> R wrap(R result, List<T> values) {
        KtoApiResponse.Items<T> items = new KtoApiResponse.Items<>();
        items.setItem(values);
        KtoApiResponse.Body<T> body = new KtoApiResponse.Body<>();
        body.setItems(items);
        KtoApiResponse.Response<T> response = new KtoApiResponse.Response<>();
        response.setBody(body);
        result.setResponse(response);
        return result;
    }
}
