package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.AreaCulResDemItem;
import com.wordiga.client.dto.AreaCulResDemResponse;
import com.wordiga.client.dto.AreaExpDivItem;
import com.wordiga.client.dto.AreaExpDivResponse;
import com.wordiga.client.dto.AreaTarExpDsItem;
import com.wordiga.client.dto.AreaTarExpDsResponse;
import com.wordiga.client.dto.AreaTarSjrnDsItem;
import com.wordiga.client.dto.AreaTarSjrnDsResponse;
import com.wordiga.client.dto.AreaTarSvcDemItem;
import com.wordiga.client.dto.AreaTarSvcDemResponse;
import com.wordiga.client.dto.AreaTouDivItem;
import com.wordiga.client.dto.AreaTouDivResponse;
import com.wordiga.client.dto.KtoApiResponse;
import com.wordiga.client.dto.TatsCnctrRateItem;
import com.wordiga.client.dto.TatsCnctrRateResponse;
import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.tourismContent.detail.SatisfactionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourismSatisfactionServiceTest {

    @Mock
    private TourismApiClient tourismApiClient;

    @InjectMocks
    private TourismSatisfactionService service;

    @Test
    void calculatesWeightedSatisfaction() {
        ContentDetailDto content = content();
        mockPopularity();
        mockAgeFit();
        mockStayFit();
        mockComfort(LocalDate.of(2026, 8, 20));

        SatisfactionDto result = service.calculate(
                content, LocalDate.of(2026, 8, 20), List.of("20S"));

        assertThat(result.getPopularityScore().getScore()).isEqualByComparingTo("90.0");
        assertThat(result.getAgeFitScore().getScore()).isEqualByComparingTo("80.0");
        assertThat(result.getStayFitScore().getScore()).isEqualByComparingTo("70.0");
        assertThat(result.getComfortScore().getScore()).isEqualByComparingTo("80.0");
        assertThat(result.getTotalScore()).isEqualByComparingTo("81.0");
    }

    @Test
    void usesNeutralScoreWhenAgeInputIsMissing() {
        ContentDetailDto content = content();
        mockPopularity();
        mockStayFit();
        mockComfort(LocalDate.of(2026, 8, 20));

        SatisfactionDto result = service.calculate(
                content, LocalDate.of(2026, 8, 20), List.of());

        assertThat(result.getAgeFitScore().isImputed()).isTrue();
        assertThat(result.getAgeFitScore().getScore()).isEqualByComparingTo("50");
    }

    private void mockPopularity() {
        AreaCulResDemItem resource = new AreaCulResDemItem();
        resource.setCulResDemIxVal("80");
        AreaCulResDemResponse resourceResponse = wrap(
                new AreaCulResDemResponse(), List.of(resource));
        when(tourismApiClient.fetchCulturalResourceDemand(
                anyString(), eq("44"), eq("44200"), eq(null)))
                .thenReturn(resourceResponse);

        AreaTarSvcDemItem serviceDemand = new AreaTarSvcDemItem();
        serviceDemand.setTarSvcDemIxVal("100");
        AreaTarSvcDemResponse serviceResponse = wrap(
                new AreaTarSvcDemResponse(), List.of(serviceDemand));
        when(tourismApiClient.fetchServiceDemand(
                anyString(), eq("44"), eq("44200"), eq(null)))
                .thenReturn(serviceResponse);
    }

    private void mockAgeFit() {
        AreaTouDivItem tourist = new AreaTouDivItem();
        tourist.setTouDivIxCd("3102");
        tourist.setTouDivIxVal("70");
        when(tourismApiClient.fetchTouristDiversity(
                anyString(), eq("44"), eq("44200"), eq(null)))
                .thenReturn(wrap(new AreaTouDivResponse(), List.of(tourist)));

        AreaExpDivItem expenditure = new AreaExpDivItem();
        expenditure.setExpDivIxCd("3202");
        expenditure.setExpDivIxVal("90");
        when(tourismApiClient.fetchExpenditureDiversity(
                anyString(), eq("44"), eq("44200"), eq(null)))
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

    private ContentDetailDto content() {
        ContentDetailDto content = new ContentDetailDto();
        content.setContentid("126508");
        content.setTitle("현충사");
        content.setLDongRegnCd("44");
        content.setLDongSignguCd("200");
        return content;
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
