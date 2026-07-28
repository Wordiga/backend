package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.AreaBasedItem;
import com.wordiga.client.dto.AreaBasedResponse;
import com.wordiga.client.dto.AreaTarExpDsItem;
import com.wordiga.client.dto.AreaTarExpDsResponse;
import com.wordiga.client.dto.AreaTarSjrnDsItem;
import com.wordiga.client.dto.AreaTarSjrnDsResponse;
import com.wordiga.client.dto.KtoApiResponse;
import com.wordiga.config.TourismProperties;
import com.wordiga.dto.tourismContent.ListType;
import com.wordiga.dto.tourismContent.TourismContentListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourismContentServiceUnitTest {

    @Mock
    private TourismApiClient tourismApiClient;

    private TourismContentService tourismContentService;

    @BeforeEach
    void setUp() {
        TourismProperties properties = new TourismProperties();
        properties.getRegion().setChungnamCode("44");
        tourismContentService = new TourismContentService(tourismApiClient, properties);
    }

    @Test
    void searchesChungnamContentWithPagination() {
        AreaBasedItem item = new AreaBasedItem();
        item.setContentid("126508");
        item.setContenttypeid("12");
        item.setTitle("현충사");
        item.setAddr1("충청남도 아산시");
        item.setLDongSignguCd("200");
        item.setMapx("126.9891281");
        item.setMapy("36.8051452");

        AreaBasedResponse response = response(List.of(item), 21);
        when(tourismApiClient.searchContent("현충사", "12", "44", "200", 1, 20))
                .thenReturn(response);

        TourismContentListResponse result = tourismContentService.getContentList(
                ListType.POPULAR, null, " 현충사 ", "12", "200", 0, 20);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getContentId()).isEqualTo("126508");
        assertThat(result.getItems().getFirst().getFirstImage()).isNull();
        assertThat(result.isHasNext()).isTrue();
    }

    @Test
    void usesPreviousYearMonthForSeasonalDemand() {
        tourismContentService.getContentList(
                ListType.SEASONAL, LocalDate.of(2026, 8, 20), null, null, null, 0, 20);

        verify(tourismApiClient).fetchServiceDemand("202508", "44", null, "11");
    }

    @Test
    void ranksPopularRegionsAndMapsAllContentCategories() {
        AreaTarExpDsItem expenditure = new AreaTarExpDsItem();
        expenditure.setSignguCd("44200");
        expenditure.setTarExpDsIxVal("90");
        AreaTarSjrnDsItem stay = new AreaTarSjrnDsItem();
        stay.setSignguCd("44200");
        stay.setTarSjrnDsIxVal("80");
        when(tourismApiClient.fetchExpenditureIntensity(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("44"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq("2201")))
                .thenReturn(wrap(new AreaTarExpDsResponse(), List.of(expenditure)));
        when(tourismApiClient.fetchStayIntensity(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("44"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq("2101")))
                .thenReturn(wrap(new AreaTarSjrnDsResponse(), List.of(stay)));
        List<AreaBasedItem> contents = List.of(
                item("12"), item("14"), item("15"), item("25"), item("28"),
                item("32"), item("38"), item("39"), item("unknown"));
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 50)).thenReturn(contents);

        TourismContentListResponse result = tourismContentService.getContentList(
                ListType.POPULAR, null, null, null, null, 0, 20);

        assertThat(result.getItems()).hasSize(9);
        assertThat(result.getItems()).extracting("categoryName")
                .containsExactly("관광지", "문화시설", "행사/공연/축제", "여행코스", "레포츠",
                        "숙박", "쇼핑", "음식점", "기타");
    }

    private AreaBasedItem item(String contentTypeId) {
        AreaBasedItem item = new AreaBasedItem();
        item.setContentid(contentTypeId);
        item.setContenttypeid(contentTypeId);
        item.setTitle(contentTypeId);
        return item;
    }

    private AreaBasedResponse response(List<AreaBasedItem> items, int totalCount) {
        KtoApiResponse.Items<AreaBasedItem> responseItems = new KtoApiResponse.Items<>();
        responseItems.setItem(items);
        KtoApiResponse.Body<AreaBasedItem> body = new KtoApiResponse.Body<>();
        body.setItems(responseItems);
        body.setTotalCount(totalCount);
        KtoApiResponse.Response<AreaBasedItem> responseBody = new KtoApiResponse.Response<>();
        responseBody.setBody(body);
        AreaBasedResponse response = new AreaBasedResponse();
        response.setResponse(responseBody);
        return response;
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
