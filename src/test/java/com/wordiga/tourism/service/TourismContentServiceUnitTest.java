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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
                personalizedTourismContentService, detailService, wishRepository);
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
        assertThat(result.getItems().getFirst().getEstimatedCost()).isEqualTo(10_000);
        assertThat(result.getItems().getFirst().getCost().unitAmount()).isEqualTo(10_000);
        assertThat(result.getItems().getFirst().getCost().totalAmount()).isEqualTo(100_000);
        assertThat(result.getItems().getFirst().getCost().source()).isEqualTo(com.wordiga.plan.CostSource.DEFAULT);
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getTotalCount()).isEqualTo(21);
        assertThat(result.getTotalPages()).isEqualTo(2);
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
        TourismContentListResponse result = tourismContentService.getContentList(1L, ListType.POPULAR, null,
                "충남", null, null, null, null, List.of("30S"), 0, 20);

        assertThat(result.getItems()).extracting("wished").containsExactly(true, false);
        assertThat(result.getItems()).extracting("recommendationScore").doesNotContainNull();
    }

    @Test
    void usesPreviousYearMonthForSeasonalDemand() {
        tourismContentService.getContentList(null,
                ListType.SEASONAL, LocalDate.of(2026, 8, 20), null, null, null, null, null, null, 0, 20);

        verify(tourismApiClient).fetchServiceDemand("202508", "44", null, "11");
    }

    @ParameterizedTest
    @CsvSource({
            "4,12,93.0", "4,14,78.0",
            "7,28,93.0", "7,32,78.0",
            "10,14,93.0", "10,32,78.0",
            "1,32,93.0", "1,12,78.0"
    })
    void combinesRegionalDemandWithSeasonalContentTypeFit(int month, String contentType, String expectedScore) {
        AreaTarSvcDemItem demand = new AreaTarSvcDemItem();
        demand.setSignguCd("44200");
        demand.setTarSvcDemIxVal("90");
        String baseYm = "2025" + String.format("%02d", month);
        when(tourismApiClient.fetchServiceDemand(baseYm, "44", null, "11"))
                .thenReturn(wrap(new AreaTarSvcDemResponse(), List.of(demand)));
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 50))
                .thenReturn(List.of(item(contentType)));

        TourismContentListResponse result = tourismContentService.getContentList(null, ListType.SEASONAL,
                LocalDate.of(2026, month, 1), null, null, null, null, null, null, 0, 20);

        assertThat(result.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getRecommendationScore()).isEqualByComparingTo(expectedScore));
    }

    @Test
    void festivalRecommendationExcludesOrdinaryContents() {
        AreaTarSvcDemItem demand = new AreaTarSvcDemItem();
        demand.setSignguCd("44200");
        demand.setTarSvcDemIxVal("90");
        when(tourismApiClient.fetchServiceDemand("202507", "44", null, "11"))
                .thenReturn(wrap(new AreaTarSvcDemResponse(), List.of(demand)));
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 50))
                .thenReturn(List.of(item("12"), item("15")));

        TourismContentListResponse result = tourismContentService.getContentList(null, ListType.FESTIVAL,
                LocalDate.of(2026, 7, 1), null, null, null, null, null, null, 0, 20);

        assertThat(result.getItems()).extracting("contentTypeId").containsExactly("15");
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void ranksPopularRegions() {
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

        assertThat(result.getItems()).hasSize(9);
        assertThat(result.getItems().getFirst().getRecommendationScore()).isEqualByComparingTo("86");
    }

    @Test
    void mapsPolicyThemeAndCategoryAndProvidesMetadata() {
        AreaBasedItem festival = item("15");
        festival.setLclsSystm1("EV");
        AreaBasedItem condominium = item("32");
        condominium.setLclsSystm1("AC");
        condominium.setLclsSystm2("AC02");
        AreaBasedItem western = item("39");
        western.setLclsSystm1("FD");
        western.setLclsSystm2("FD02");
        western.setLclsSystm3("FD020300");
        when(tourismApiClient.searchContent("분류", null, "44", null, 1, 20))
                .thenReturn(response(List.of(festival, condominium, western), 3));

        TourismContentListResponse result = tourismContentService.getContentList(null, ListType.POPULAR,
                null, "분류", null, null, null, null, null, null, 0, 20);

        assertThat(result.getItems()).extracting(item -> item.getTheme().name())
                .containsExactly("숙소", "맛집/카페");
        assertThat(result.getItems()).extracting(item -> item.getCategory().name())
                .containsExactly("콘도미니엄", "양식");
        assertThat(tourismContentService.getCategories()).hasSize(3)
                .extracting(group -> group.categories().size())
                .containsExactly(7, 6, 9);
    }

    @Test
    void appliesThemeAndMultipleCategoryFilters() {
        mockPopularRegion(List.of(classified("history", "HS"), classified("nature", "NA"),
                classified("food", "FD01")));

        TourismContentListResponse result = tourismContentService.getContentList(null, ListType.POPULAR,
                LocalDate.of(2026, 9, 1), null, null, "ATTRACTION_EXPERIENCE", List.of("HS,NA"),
                null, null, null, 10, null, 0, 20);

        assertThat(result.getItems()).extracting("contentId").containsExactlyInAnyOrder("history", "nature");
    }

    @Test
    void handlesBlankAndUnknownRecommendationFilters() {
        AreaBasedItem unknown = item("12");
        unknown.setContentid("unknown");
        unknown.setLDongSignguCd("200");
        AreaBasedItem food = classified("food", "unused");
        food.setLclsSystm2("FD01");
        mockPopularRegion(List.of(classified("history", "HS"), unknown, food));

        var foodOnly = tourismContentService.getContentList(null, ListType.POPULAR, LocalDate.of(2026, 9, 1),
                null, null, "FOOD_CAFE", null, null, null, null, 10, null, 0, 20);
        var historyOnly = tourismContentService.getContentList(null, ListType.POPULAR, LocalDate.of(2026, 9, 1),
                null, null, " ", java.util.Arrays.asList(null, " ", "HS"), null, null, null, 10, null, 0, 20);
        var blankCategory = tourismContentService.getContentList(null, ListType.POPULAR, LocalDate.of(2026, 9, 1),
                null, null, null, List.of(""), null, null, null, 10, null, 0, 20);

        assertThat(foodOnly.getItems()).extracting("contentId").containsExactly("food");
        assertThat(historyOnly.getItems()).extracting("contentId").containsExactly("history");
        assertThat(blankCategory.getItems()).hasSize(3);
    }

    @Test
    void popularAndSeasonalUseDifferentStableOrdering() {
        List<AreaBasedItem> contents = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> classified("content-" + index, "HS")).toList();
        mockPopularRegion(contents);
        AreaTarSvcDemItem demand = new AreaTarSvcDemItem();
        demand.setSignguCd("44200");
        demand.setTarSvcDemIxVal("90");
        when(tourismApiClient.fetchServiceDemand("202509", "44", null, "11"))
                .thenReturn(wrap(new AreaTarSvcDemResponse(), List.of(demand)));

        var popular = tourismContentService.getContentList(null, ListType.POPULAR, LocalDate.of(2026, 9, 1),
                null, null, null, null, null, null, null, 0, 6);
        var seasonal = tourismContentService.getContentList(null, ListType.SEASONAL, LocalDate.of(2026, 9, 1),
                null, null, null, null, null, null, null, 0, 6);

        var popularIds = popular.getItems().stream().map(item -> item.getContentId()).toList();
        var seasonalIds = seasonal.getItems().stream().map(item -> item.getContentId()).toList();
        assertThat(popularIds).isNotEqualTo(seasonalIds);
    }

    @ParameterizedTest
    @CsvSource({"4,12,93.0", "7,28,93.0", "12,32,93.0"})
    void appliesSeasonSpecificContentScore(int month, String contentTypeId, String expectedScore) {
        AreaTarSvcDemItem demand = new AreaTarSvcDemItem();
        demand.setSignguCd("44200");
        demand.setTarSvcDemIxVal("90");
        when(tourismApiClient.fetchServiceDemand(anyString(), eq("44"), isNull(), eq("11")))
                .thenReturn(wrap(new AreaTarSvcDemResponse(), List.of(demand)));
        AreaBasedItem content = item(contentTypeId);
        content.setContentid("seasonal");
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 50)).thenReturn(List.of(content));

        var result = tourismContentService.getContentList(null, ListType.SEASONAL,
                LocalDate.of(2026, month, 1), null, null, null, null, null, null, null, 0, 6);

        assertThat(result.getItems().getFirst().getRecommendationScore()).isEqualByComparingTo(expectedScore);
    }

    @Test
    void returnsFestivalsOnlyThroughFestivalType() {
        AreaBasedItem festival = item("15");
        when(tourismApiClient.searchContent("가을", "15", "44", null, 1, 20))
                .thenReturn(response(List.of(festival), 1));

        TourismContentListResponse result = tourismContentService.getContentList(null, ListType.FESTIVAL,
                LocalDate.of(2026, 10, 1), "가을", null, null, null, null, null, null, 0, 20);

        assertThat(result.getItems()).extracting("contentTypeId").containsExactly("15");
        assertThat(result.getTotalCount()).isEqualTo(1);
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
    void enrichesPersonalizedRecommendationsWithWishes() {
        var wished = com.wordiga.tourism.dto.TourismContentDto.builder().contentId("wished").build();
        var other = com.wordiga.tourism.dto.TourismContentDto.builder().contentId("other").build();
        TourismContentListResponse recommendations = TourismContentListResponse.builder()
                .items(List.of(wished, other)).page(0).size(20).hasNext(false).build();
        when(personalizedTourismContentService.get(
                1L, LocalDate.of(2026, 8, 20), List.of("30S"), null, null, null, List.of(), 0, 20))
                .thenReturn(recommendations);
        when(wishRepository.findByMemberIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(Wish.create(1L,
                snapshot("wished", "위시 콘텐츠", null), "기본 위시리스트")));

        var result = tourismContentService.getContentList(1L, ListType.PERSONALIZED, LocalDate.of(2026, 8, 20),
                null, null, null, null, null, List.of("30S"), 0, 20);

        assertThat(result.getItems()).extracting("wished").containsExactly(true, false);
    }

    private AreaBasedItem classified(String id, String large) {
        AreaBasedItem item = item("12");
        item.setContentid(id);
        item.setLclsSystm1(large);
        item.setLDongSignguCd("200");
        return item;
    }

    private void mockPopularRegion(List<AreaBasedItem> contents) {
        AreaTarExpDsItem expenditure = new AreaTarExpDsItem();
        expenditure.setSignguCd("44200");
        expenditure.setTarExpDsIxVal("90");
        AreaTarSjrnDsItem stay = new AreaTarSjrnDsItem();
        stay.setSignguCd("44200");
        stay.setTarSjrnDsIxVal("80");
        when(tourismApiClient.fetchExpenditureIntensity(anyString(), eq("44"), isNull(), eq("2201")))
                .thenReturn(wrap(new AreaTarExpDsResponse(), List.of(expenditure)));
        when(tourismApiClient.fetchStayIntensity(anyString(), eq("44"), isNull(), eq("2103")))
                .thenReturn(wrap(new AreaTarSjrnDsResponse(), List.of(stay)));
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 50)).thenReturn(contents);
    }

    @Test
    void resolvesRelatedTourismNamesToChungnamContentIds() {
        ContentDetailDto reference = new ContentDetailDto();
        reference.setContentid("2717354");
        reference.setTitle("신라스테이 천안");
        reference.setLDongRegnCd("44");
        reference.setLDongSignguCd("133");
        when(tourismApiClient.fetchCommonDetail("2717354")).thenReturn(reference);
        RelatedTourismItem related = new RelatedTourismItem();
        related.setRlteTatsNm("독립기념관");
        related.setRlteRank(1);
        when(tourismApiClient.fetchRelatedTourism(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("44"), org.mockito.ArgumentMatchers.eq("44133"),
                org.mockito.ArgumentMatchers.eq("신라스테이 천안"), org.mockito.ArgumentMatchers.eq(50)))
                .thenReturn(List.of(related));
        AreaBasedItem matched = item("14");
        matched.setContentid("129790");
        matched.setTitle("독립기념관");
        when(tourismApiClient.searchContent("독립기념관", null, "44", null, 1, 10))
                .thenReturn(response(List.of(matched), 1));

        TourismContentListResponse result = tourismContentService.getContentList(null, ListType.RELATED, null,
                null, null, null, "2717354", null, null, null, 0, 20);

        assertThat(result.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getContentId()).isEqualTo("129790"));
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
