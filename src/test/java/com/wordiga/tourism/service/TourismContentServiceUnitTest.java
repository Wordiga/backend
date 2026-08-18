package com.wordiga.tourism.service;

import com.wordiga.tourism.dto.ListType;
import com.wordiga.tourism.dto.TourismContentListResponse;
import com.wordiga.tourism.dto.detail.SatisfactionDto;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.*;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.wish.repository.WishRepository;
import com.wordiga.wish.Wish;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourismContentServiceUnitTest {

    @Mock
    private TourismApiClient tourismApiClient;
    @Mock
    private PersonalizedTourismContentService personalizedTourismContentService;
    @Mock
    private TourismContentDetailService detailService;
    @Mock
    private TourismSatisfactionService satisfactionService;
    @Mock
    private WishRepository wishRepository;

    private TourismContentService tourismContentService;

    @BeforeEach
    void setUp() {
        TourismProperties properties = new TourismProperties();
        properties.getRegion().setChungnamCode("44");
        tourismContentService = new TourismContentService(tourismApiClient, properties,
                personalizedTourismContentService, detailService, satisfactionService, wishRepository);
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

        TourismContentListResponse result = tourismContentService.getContentList(null,
                ListType.POPULAR, null, " 현충사 ", "12", "200", null, null, null, 0, 20);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getContentId()).isEqualTo("126508");
        assertThat(result.getItems().getFirst().getFirstImage()).isNull();
        assertThat(result.isHasNext()).isTrue();
        verifyNoInteractions(satisfactionService);
    }

    @Test
    void marksContentsWishedForAuthenticatedMember() {
        AreaBasedItem wished = item("12");
        wished.setContentid("wished");
        wished.setLDongSignguCd("200");
        AreaBasedItem unwished = item("14");
        unwished.setContentid("unwished");
        unwished.setLDongSignguCd("200");
        when(tourismApiClient.searchContent("충남", null, "44", null, 1, 20))
                .thenReturn(response(List.of(wished, unwished), 2));
        when(wishRepository.findByMemberIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(
                Wish.create(1L, snapshot("wished", "위시", null), "아산시")));
        SatisfactionDto satisfaction = SatisfactionDto.builder().totalScore(java.math.BigDecimal.valueOf(87.5)).build();
        when(satisfactionService.calculate(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(satisfaction);

        TourismContentListResponse result = tourismContentService.getContentList(1L, ListType.POPULAR, null,
                "충남", null, null, null, null, List.of("30S"), 0, 20);

        assertThat(result.getItems()).extracting("wished").containsExactly(true, false);
        assertThat(result.getItems()).extracting("satisfaction").containsOnly(satisfaction);
    }

    @Test
    void usesPreviousYearMonthForSeasonalDemand() {
        tourismContentService.getContentList(null,
                ListType.SEASONAL, LocalDate.of(2026, 8, 20), null, null, null, null, null, null, 0, 20);

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
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq("2103")))
                .thenReturn(wrap(new AreaTarSjrnDsResponse(), List.of(stay)));
        AreaBasedItem nullType = item(null);
        nullType.setContentid("null-type");
        List<AreaBasedItem> contents = List.of(
                item("12"), item("14"), item("15"), item("25"), item("28"),
                item("32"), item("38"), item("39"), item("unknown"), nullType);
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 50)).thenReturn(contents);

        TourismContentListResponse result = tourismContentService.getContentList(null,
                ListType.POPULAR, null, null, null, null, null, null, null, 0, 20);

        assertThat(result.getItems()).hasSize(10);
        assertThat(result.getItems().getFirst().getRecommendationScore()).isEqualByComparingTo("86");
        assertThat(result.getItems()).extracting("categoryName")
                .containsExactly("관광지", "문화시설", "행사/공연/축제", "여행코스", "레포츠",
                        "숙박", "쇼핑", "음식점", "기타", "기타");
    }

    @Test
    void treatsBlankKeywordAsRecommendationRequest() {
        TourismContentListResponse result = tourismContentService.getContentList(null,
                ListType.POPULAR, null, " ", null, null, null, null, null, 0, 20);

        assertThat(result.getItems()).isEmpty();
        verify(tourismApiClient, org.mockito.Mockito.never())
                .searchContent(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void filtersSeasonalContentsAndRemovesDuplicateContentIds() {
        AreaTarSvcDemItem demand = new AreaTarSvcDemItem();
        demand.setSignguCd("44200");
        demand.setTarSvcDemIxVal("90");
        when(tourismApiClient.fetchServiceDemand("202508", "44", null, "11"))
                .thenReturn(wrap(new AreaTarSvcDemResponse(), List.of(demand)));
        AreaBasedItem matched = item("12");
        matched.setContentid("same");
        matched.setLDongSignguCd("200");
        matched.setMapx("invalid");
        matched.setMapy("");
        AreaBasedItem duplicate = item("12");
        duplicate.setContentid("same");
        duplicate.setLDongSignguCd("200");
        AreaBasedItem otherType = item("14");
        otherType.setContentid("other-type");
        otherType.setLDongSignguCd("200");
        AreaBasedItem otherRegion = item("12");
        otherRegion.setContentid("other-region");
        otherRegion.setLDongSignguCd("150");
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 50))
                .thenReturn(List.of(matched, duplicate, otherType, otherRegion));

        TourismContentListResponse result = tourismContentService.getContentList(null,
                ListType.SEASONAL, LocalDate.of(2026, 8, 20),
                null, "12", "200", null, null, null, 0, 20);

        assertThat(result.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getContentId()).isEqualTo("same");
                    assertThat(item.getMapx()).isNull();
                    assertThat(item.getMapy()).isNull();
                });
    }

    @Test
    void delegatesPersonalizedRecommendationsWithoutReloadingWishes() {
        TourismContentListResponse empty = TourismContentListResponse.builder()
                .items(List.of()).page(0).size(20).hasNext(false).build();
        when(personalizedTourismContentService.get(
                1L, LocalDate.of(2026, 8, 20), List.of("30S"), null, 0, 20)).thenReturn(empty);

        assertThat(tourismContentService.getContentList(1L, ListType.PERSONALIZED, LocalDate.of(2026, 8, 20),
                null, null, null, null, null, List.of("30S"), 0, 20)).isSameAs(empty);
        verify(wishRepository, org.mockito.Mockito.never()).findByMemberIdOrderByCreatedAtDescIdDesc(1L);
    }

    @Test
    void filtersSearchResultsToLodgingThatCanAccommodateParticipants() {
        AreaBasedItem lodging = item("32");
        lodging.setContentid("lodging");
        AreaBasedItem tourist = item("12");
        tourist.setContentid("tourist");
        when(tourismApiClient.searchContent("숙소", null, "44", null, 1, 20))
                .thenReturn(response(List.of(lodging, tourist), 2));
        when(detailService.getDetail("lodging", null, null, null, 25)).thenReturn(
                com.wordiga.tourism.dto.detail.TourismContentDetailResponse.builder()
                        .capacitySatisfied(true).build());

        TourismContentListResponse result = tourismContentService.getContentList(1L, ListType.POPULAR, null,
                "숙소", null, null, true, 25, null, 0, 20);

        assertThat(result.getItems()).extracting("contentId").containsExactly("lodging");
    }

    @Test
    void defaultsParticipantCountToTenForCapacityFilter() {
        AreaBasedItem lodging = item("32");
        lodging.setContentid("lodging");
        when(tourismApiClient.searchContent("숙소", null, "44", null, 1, 20))
                .thenReturn(response(List.of(lodging), 1));
        when(detailService.getDetail("lodging", null, null, null, 10)).thenReturn(
                com.wordiga.tourism.dto.detail.TourismContentDetailResponse.builder()
                        .capacitySatisfied(true).build());

        TourismContentListResponse result = tourismContentService.getContentList(1L, ListType.POPULAR, null,
                "숙소", null, null, true, null, null, 0, 20);

        assertThat(result.getItems()).extracting("contentId").containsExactly("lodging");
    }

    private AreaBasedItem item(String contentTypeId) {
        AreaBasedItem item = new AreaBasedItem();
        item.setContentid(contentTypeId);
        item.setContenttypeid(contentTypeId);
        item.setTitle(contentTypeId);
        return item;
    }

    private TourismContentSnapshot snapshot(String contentId, String title, String image) {
        return TourismContentSnapshot.builder()
                .contentId(contentId)
                .contentTypeId("12")
                .title(title)
                .firstimage(image)
                .sigunguCode("200")
                .sigunguName("아산시")
                .updatedAt(java.time.LocalDateTime.now())
                .build();
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
