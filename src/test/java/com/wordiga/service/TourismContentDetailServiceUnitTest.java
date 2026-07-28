package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.AreaTarExpDsItem;
import com.wordiga.client.dto.AreaTarExpDsResponse;
import com.wordiga.client.dto.KtoApiResponse;
import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.DetailImageDto;
import com.wordiga.dto.DetailInfoDto;
import com.wordiga.dto.DetailIntroDto;
import com.wordiga.dto.tourismContent.detail.SeasonalImageDto;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourismContentDetailServiceUnitTest {

    @Mock
    private WorkshopDetailService workshopDetailService;

    @Mock
    private TourismApiClient tourismApiClient;

    @Spy
    private TourismDetailMapper detailMapper = new TourismDetailMapper();

    @Mock
    private TourismSatisfactionService satisfactionService;

    @InjectMocks
    private TourismContentDetailService service;

    @Test
    void combinesDetailResponses() {
        ContentDetailDto common = common("44");
        DetailIntroDto intro = new DetailIntroDto();
        intro.setContentid("126508");
        intro.setContenttypeid("12");
        intro.setUsetime("09:00~18:00");
        DetailInfoDto detail = new DetailInfoDto();
        detail.setContentid("126508");
        detail.setContenttypeid("12");
        detail.setInfoname("입장료");
        detail.setInfotext("무료");
        DetailImageDto image = new DetailImageDto();
        image.setOriginimgurl("https://example.com/image.jpg");
        image.setSmallimageurl("https://example.com/thumb.jpg");

        when(workshopDetailService.fetchCommonDetail("126508")).thenReturn(common);
        when(workshopDetailService.fetchIntroDetail("126508", "12")).thenReturn(intro);
        when(workshopDetailService.fetchRepeatInfo("126508", "12", 1, 100))
                .thenReturn(List.of(detail));
        when(workshopDetailService.fetchImages("126508", "Y", 1, 100))
                .thenReturn(List.of(image));
        when(tourismApiClient.fetchExpenditureIntensity(
                anyString(), eq("44"), eq("44200"), eq("2201")))
                .thenReturn(spendingResponse());

        TourismContentDetailResponse result = service.getDetail("126508", null, List.of("20S"));

        assertThat(result.getCommon().getTitle()).isEqualTo("현충사");
        assertThat(result.getIntro().getUseTime()).isEqualTo("09:00~18:00");
        assertThat(result.getDetails()).singleElement()
                .satisfies(item -> assertThat(item.getInfoName()).isEqualTo("입장료"));
        assertThat(result.getSpendingIndex().getIndexValue()).isEqualByComparingTo("112.4");
        assertThat(result.getSeasonalImages()).singleElement()
                .satisfies(item -> assertThat(item.getSeason()).isEqualTo(SeasonalImageDto.Season.UNKNOWN));
    }

    @ParameterizedTest
    @CsvSource({
            "12,관광지",
            "14,문화시설",
            "15,행사/공연/축제",
            "25,여행코스",
            "28,레포츠",
            "32,숙박",
            "38,쇼핑",
            "39,음식점",
            "99,기타"
    })
    void mapsEverySpendingCategory(String contentTypeId, String categoryName) {
        ContentDetailDto common = common("44");
        common.setContenttypeid(contentTypeId);
        when(workshopDetailService.fetchCommonDetail(contentTypeId)).thenReturn(common);
        when(workshopDetailService.fetchRepeatInfo(contentTypeId, contentTypeId, 1, 100))
                .thenReturn(List.of());
        when(workshopDetailService.fetchImages(contentTypeId, "Y", 1, 100))
                .thenReturn(List.of());
        when(tourismApiClient.fetchExpenditureIntensity(
                anyString(), eq("44"), eq("44200"), eq("2201")))
                .thenReturn(spendingResponse());

        TourismContentDetailResponse result = service.getDetail(contentTypeId, null, List.of());

        assertThat(result.getSpendingIndex().getCategoryName()).isEqualTo(categoryName);
    }

    @Test
    void handlesMissingSpendingDataAndMalformedCoordinates() {
        ContentDetailDto common = common("44");
        common.setLDongSignguCd(null);
        common.setMapx("invalid");
        common.setMapy("");
        when(workshopDetailService.fetchCommonDetail("126508")).thenReturn(common);
        when(workshopDetailService.fetchRepeatInfo("126508", "12", 1, 100))
                .thenReturn(List.of());
        when(workshopDetailService.fetchImages("126508", "Y", 1, 100))
                .thenReturn(List.of());
        when(tourismApiClient.fetchExpenditureIntensity(
                anyString(), eq("44"), eq(null), eq("2201")))
                .thenReturn(null);

        TourismContentDetailResponse result = service.getDetail("126508", null, List.of());

        assertThat(result.getCommon().getMapx()).isNull();
        assertThat(result.getCommon().getMapy()).isNull();
        assertThat(result.getSpendingIndex()).isNull();
    }

    private ContentDetailDto common(String regionCode) {
        ContentDetailDto common = new ContentDetailDto();
        common.setContentid("126508");
        common.setContenttypeid("12");
        common.setTitle("현충사");
        common.setLDongRegnCd(regionCode);
        common.setLDongSignguCd("200");
        common.setMapx("126.9891281");
        common.setMapy("36.8051452");
        return common;
    }

    private AreaTarExpDsResponse spendingResponse() {
        AreaTarExpDsItem item = new AreaTarExpDsItem();
        item.setBaseYm("202606");
        item.setSignguNm("아산시");
        item.setTarExpDsIxVal("112.4");
        KtoApiResponse.Items<AreaTarExpDsItem> items = new KtoApiResponse.Items<>();
        items.setItem(List.of(item));
        KtoApiResponse.Body<AreaTarExpDsItem> body = new KtoApiResponse.Body<>();
        body.setItems(items);
        KtoApiResponse.Response<AreaTarExpDsItem> response = new KtoApiResponse.Response<>();
        response.setBody(body);
        AreaTarExpDsResponse result = new AreaTarExpDsResponse();
        result.setResponse(response);
        return result;
    }
}
