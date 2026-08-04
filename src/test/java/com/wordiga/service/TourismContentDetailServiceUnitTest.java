package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.AreaTarExpDsItem;
import com.wordiga.client.dto.AreaTarExpDsResponse;
import com.wordiga.client.dto.KtoApiResponse;
import com.wordiga.client.dto.PhotoGalleryItem;
import com.wordiga.client.dto.PhotoGalleryResponse;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        image.setSerialnum("7");

        when(workshopDetailService.fetchCommonDetail("126508")).thenReturn(common);
        when(workshopDetailService.fetchIntroDetail("126508", "12")).thenReturn(intro);
        when(workshopDetailService.fetchRepeatInfo("126508", "12", 1, 100))
                .thenReturn(List.of(detail));
        when(workshopDetailService.fetchImages("126508", "Y", 1, 100))
                .thenReturn(List.of(image));
        when(tourismApiClient.searchPhotos("현충사")).thenReturn(photoResponse("202606"));

        TourismContentDetailResponse result = service.getDetail("126508", null, List.of("20S"), 8);

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
    void mapsExactShootingDateAndUnknownInvalidDate() {
        PhotoGalleryItem exact = new PhotoGalleryItem(); exact.setGalPhotographyMonth("20260315");
        PhotoGalleryItem invalid = new PhotoGalleryItem(); invalid.setGalPhotographyMonth("unknown");
        PhotoGalleryItem malformed = new PhotoGalleryItem(); malformed.setGalPhotographyMonth("20261340");
        PhotoGalleryItem malformedMonth = new PhotoGalleryItem(); malformedMonth.setGalPhotographyMonth("202613");

        assertThat(service.toSeasonalImage(exact)).satisfies(image -> {
            assertThat(image.getShootingDate()).isEqualTo("2026-03-15");
            assertThat(image.getSeason()).isEqualTo(SeasonalImageDto.Season.SPRING);
        });
        assertThat(service.toSeasonalImage(invalid).getSeason()).isEqualTo(SeasonalImageDto.Season.UNKNOWN);
        assertThat(service.toSeasonalImage(malformed).getSeason()).isEqualTo(SeasonalImageDto.Season.UNKNOWN);
        assertThat(service.toSeasonalImage(malformedMonth).getSeason()).isEqualTo(SeasonalImageDto.Season.UNKNOWN);
    }

    @Test
    void commonDetailRejectsMissingAndOutsideChungnam() {
        when(workshopDetailService.fetchCommonDetail("missing")).thenReturn(null);
        when(workshopDetailService.fetchCommonDetail("seoul")).thenReturn(common("11"));
        assertThatThrownBy(() -> service.getCommonDetail("missing")).hasMessageContaining("404");
        assertThatThrownBy(() -> service.getCommonDetail("seoul")).hasMessageContaining("404");
    }

    @Test
    void handlesMalformedCoordinates() {
        ContentDetailDto common = common("44");
        common.setLDongSignguCd(null);
        common.setMapx("invalid");
        common.setMapy("");
        when(workshopDetailService.fetchCommonDetail("126508")).thenReturn(common);
        when(workshopDetailService.fetchRepeatInfo("126508", "12", 1, 100))
                .thenReturn(List.of());
        when(workshopDetailService.fetchImages("126508", "Y", 1, 100))
                .thenReturn(List.of());

        TourismContentDetailResponse result = service.getDetail("126508", null, List.of(), 8);

        assertThat(result.getCommon().getMapx()).isNull();
        assertThat(result.getCommon().getMapy()).isNull();
    }

    @Test
    void reportsWhetherLodgingCanAccommodateParticipants() {
        ContentDetailDto common = common("44"); common.setContenttypeid("32");
        DetailIntroDto intro = new DetailIntroDto(); intro.setContenttypeid("32"); intro.setAccomcountlodging("10명");
        when(workshopDetailService.fetchCommonDetail("126508")).thenReturn(common);
        when(workshopDetailService.fetchIntroDetail("126508", "32")).thenReturn(intro);
        when(workshopDetailService.fetchRepeatInfo("126508", "32", 1, 100)).thenReturn(List.of());
        when(workshopDetailService.fetchImages("126508", "Y", 1, 100)).thenReturn(List.of());

        assertThat(service.getDetail("126508", null, List.of(), 8).getCapacitySatisfied()).isTrue();
        assertThat(service.getDetail("126508", null, List.of(), 11).getCapacitySatisfied()).isFalse();
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
        items.setItem(List.of(item)); body.setItems(items); response.setBody(body); result.setResponse(response);
        return result;
    }

}
