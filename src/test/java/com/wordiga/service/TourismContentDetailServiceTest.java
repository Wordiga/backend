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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourismContentDetailServiceTest {

    @Mock
    private WorkshopDetailService workshopDetailService;

    @Mock
    private TourismApiClient tourismApiClient;

    @Spy
    private TourismDetailMapper detailMapper = new TourismDetailMapper();

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

        TourismContentDetailResponse result = service.getDetail("126508");

        assertThat(result.getCommon().getTitle()).isEqualTo("현충사");
        assertThat(result.getIntro().getUseTime()).isEqualTo("09:00~18:00");
        assertThat(result.getDetails()).singleElement()
                .satisfies(item -> assertThat(item.getInfoName()).isEqualTo("입장료"));
        assertThat(result.getSpendingIndex().getIndexValue()).isEqualByComparingTo("112.4");
        assertThat(result.getSeasonalImages()).singleElement()
                .satisfies(item -> assertThat(item.getSeason()).isEqualTo(SeasonalImageDto.Season.UNKNOWN));
    }

    @Test
    void rejectsContentOutsideChungnam() {
        when(workshopDetailService.fetchCommonDetail("126508")).thenReturn(common("11"));

        assertThatThrownBy(() -> service.getDetail("126508"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
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
