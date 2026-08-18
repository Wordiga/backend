package com.wordiga.tourism.service;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.global.client.dto.DetailImageDto;
import com.wordiga.global.client.dto.DetailInfoDto;
import com.wordiga.global.client.dto.DetailIntroDto;
import com.wordiga.tourism.dto.detail.SeasonalImageDto;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.KtoApiResponse;
import com.wordiga.global.client.dto.PhotoGalleryItem;
import com.wordiga.global.client.dto.PhotoGalleryResponse;
import com.wordiga.global.config.TourismProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourismContentDetailServiceUnitTest {

    @Mock
    private TourismApiClient tourismApiClient;

    @Spy
    private TourismDetailMapper detailMapper = new TourismDetailMapper();

    @Spy
    private TourismProperties tourismProperties = tourismProperties();

    @Mock
    private TourismSatisfactionService satisfactionService;

    @Mock
    private MonthlyWeatherService monthlyWeatherService;

    @InjectMocks
    private TourismContentDetailService service;

    @Test
    void combinesDetailResponses() {
        ContentDetailDto common = common("44");
        DetailIntroDto intro = new DetailIntroDto();
        intro.setContenttypeid("12");
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
        image.setSerialnum("7");

        when(tourismApiClient.fetchCommonDetail("126508")).thenReturn(common);
        when(tourismApiClient.fetchIntroDetail("126508", "12")).thenReturn(intro);
        when(tourismApiClient.fetchRepeatInfo("126508", "12", 1, 100))
                .thenReturn(List.of(detail));
        when(tourismApiClient.fetchImages("126508", "Y", 1, 100))
                .thenReturn(List.of(image));
        when(tourismApiClient.searchPhotos("현충사")).thenReturn(photoResponse("202606"));

        TourismContentDetailResponse result = service.getDetail("126508", null, List.of("20S"), null, 8);

        assertThat(result.getCommon().getTitle()).isEqualTo("현충사");
        assertThat(result.getIntro().getUseTime()).isEqualTo("09:00~18:00");
        assertThat(result.getDetails()).singleElement()
                .satisfies(item -> assertThat(item.getInfoName()).isEqualTo("입장료"));
        assertThat(result.getImages()).singleElement()
                .satisfies(item -> assertThat(item.getSerialNumber()).isEqualTo(7));
        assertThat(result.getSeasonalImages()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getSeason()).isEqualTo(SeasonalImageDto.Season.SUMMER);
                    assertThat(item.getShootingDate()).isNull();
                });
    }

    @Test
    void commonDetailRejectsMissingAndOutsideChungnam() {
        when(tourismApiClient.fetchCommonDetail("missing")).thenReturn(null);
        when(tourismApiClient.fetchCommonDetail("seoul")).thenReturn(common("11"));
        assertThatThrownBy(() -> service.getCommonDetail("missing")).hasMessageContaining("404");
        assertThatThrownBy(() -> service.getCommonDetail("seoul")).hasMessageContaining("404");
    }

    @Test
    void handlesMalformedCoordinates() {
        ContentDetailDto common = common("44");
        common.setLDongSignguCd(null);
        common.setMapx("invalid");
        common.setMapy("");
        when(tourismApiClient.fetchCommonDetail("126508")).thenReturn(common);
        when(tourismApiClient.fetchRepeatInfo("126508", "12", 1, 100))
                .thenReturn(List.of());
        when(tourismApiClient.fetchImages("126508", "Y", 1, 100))
                .thenReturn(List.of());

        TourismContentDetailResponse result = service.getDetail("126508", null, List.of(), null, 8);

        assertThat(result.getCommon().getMapx()).isNull();
        assertThat(result.getCommon().getMapy()).isNull();
    }

    @Test
    void reportsWhetherLodgingCanAccommodateParticipants() {
        ContentDetailDto common = common("44");
        common.setContenttypeid("32");
        DetailIntroDto intro = new DetailIntroDto();
        intro.setContenttypeid("32");
        intro.setAccomcountlodging("10명");
        when(tourismApiClient.fetchCommonDetail("126508")).thenReturn(common);
        when(tourismApiClient.fetchIntroDetail("126508", "32")).thenReturn(intro);
        when(tourismApiClient.fetchRepeatInfo("126508", "32", 1, 100)).thenReturn(List.of());
        when(tourismApiClient.fetchImages("126508", "Y", 1, 100)).thenReturn(List.of());

        assertThat(service.getDetail("126508", null, List.of(), null, 8).getCapacitySatisfied()).isTrue();
        assertThat(service.getDetail("126508", null, List.of(), null, null).getCapacitySatisfied()).isTrue();
        assertThat(service.getDetail("126508", null, List.of(), null, 11).getCapacitySatisfied()).isFalse();
    }

    @Test
    void aiDetailFetchesOnlyCommonIntroAndOptionalWeather() {
        ContentDetailDto common = common("44");
        DetailIntroDto intro = new DetailIntroDto();
        intro.setContenttypeid("12");
        LocalDate visitDate = LocalDate.of(2026, 8, 20);
        when(tourismApiClient.fetchCommonDetail("126508")).thenReturn(common);
        when(tourismApiClient.fetchIntroDetail("126508", "12")).thenReturn(intro);

        TourismContentDetailResponse result = service.getAiDetail("126508", visitDate, false);

        assertThat(result.getDetails()).isEmpty();
        assertThat(result.getImages()).isEmpty();
        assertThat(result.getMonthlyWeather()).isNull();
        verify(tourismApiClient, never()).fetchRepeatInfo(anyString(), anyString(), anyInt(), anyInt());
        verify(tourismApiClient, never()).fetchImages(anyString(), anyString(), anyInt(), anyInt());
        verifyNoInteractions(satisfactionService, monthlyWeatherService);
    }

    @Test
    void aiDetailFetchesWeatherOnlyWhenRequested() {
        ContentDetailDto common = common("44");
        LocalDate visitDate = LocalDate.of(2026, 8, 20);
        when(tourismApiClient.fetchCommonDetail("126508")).thenReturn(common);

        service.getAiDetail("126508", visitDate, true);

        verify(monthlyWeatherService).estimate("200", visitDate);
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

    private PhotoGalleryResponse photoResponse(String shootingMonth) {
        PhotoGalleryItem item = new PhotoGalleryItem();
        item.setGalWebImageUrl("https://example.com/photo.jpg");
        item.setGalPhotographyMonth(shootingMonth);
        PhotoGalleryResponse result = new PhotoGalleryResponse();
        KtoApiResponse.Response<PhotoGalleryItem> response = new KtoApiResponse.Response<>();
        KtoApiResponse.Body<PhotoGalleryItem> body = new KtoApiResponse.Body<>();
        KtoApiResponse.Items<PhotoGalleryItem> items = new KtoApiResponse.Items<>();
        items.setItem(List.of(item));
        body.setItems(items);
        response.setBody(body);
        result.setResponse(response);
        return result;
    }

    private static TourismProperties tourismProperties() {
        TourismProperties properties = new TourismProperties();
        properties.getRegion().setChungnamCode("44");
        return properties;
    }

}
