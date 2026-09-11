package com.wordiga.tourism.service;

import com.wordiga.plan.dto.PlanGenerateRequest;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.AreaBasedItem;
import com.wordiga.tourism.dto.detail.TourismCommonDetailDto;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.tourism.service.RegionalContentService;
import com.wordiga.tourism.service.TourismContentDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionalContentServiceUnitTest {
    @Mock
    TourismApiClient tourismApiClient;
    @Mock
    TourismContentDetailService detailService;
    RegionalContentService service;

    @BeforeEach
    void setUp() {
        com.wordiga.global.config.TourismProperties properties = new com.wordiga.global.config.TourismProperties();
        properties.getRegion().setChungnamCode("44");
        service = new RegionalContentService(tourismApiClient, properties, detailService);
    }

    @Test
    void selectsNearbyRegionalContentAndExcludesSavedContent() {
        PlanGenerateRequest request = request();
        AreaBasedItem duplicate = candidate("126508", "현충사");
        AreaBasedItem candidate = candidate("regional-1", "외암민속마을");
        when(tourismApiClient.fetchAreaBasedContent("44", "460", 1000))
                .thenReturn(List.of(duplicate, candidate));
        when(tourismApiClient.fetchAreaBasedContent("44", null, 1000)).thenReturn(List.of());
        TourismContentDetailResponse enriched = detail("regional-1", "외암민속마을");
        when(detailService.getAiDetail("regional-1", LocalDate.of(2026, 8, 1)))
                .thenReturn(enriched);

        assertThat(service.find(request, List.of(detail("126508", "현충사"))))
                .extracting(item -> item.getCommon().getContentId()).containsExactly("regional-1");
        verify(detailService, never()).getAiDetail(eq("126508"), any());
    }

    @Test
    void excludesRegionalContentMissingAiRequiredFields() {
        PlanGenerateRequest request = request();
        AreaBasedItem candidate = candidate("regional-1", "외암민속마을");
        when(tourismApiClient.fetchAreaBasedContent("44", "460", 1000)).thenReturn(List.of(candidate));
        when(tourismApiClient.fetchAreaBasedContent("44", null, 1000)).thenReturn(List.of());
        when(detailService.getAiDetail("regional-1", LocalDate.of(2026, 8, 1)))
                .thenReturn(TourismContentDetailResponse.builder().common(TourismCommonDetailDto.builder()
                        .contentId("regional-1").title("외암민속마을").lDongSignguCd("460")
                        .mapx(java.math.BigDecimal.valueOf(126.9))
                        .mapy(java.math.BigDecimal.valueOf(36.8)).build()).build());

        assertThat(service.find(request, List.of(detail("126508", "현충사")))).isEmpty();
    }

    @Test
    void balancesFifteenCandidatesAndFillsLocalShortagesFromChungnam() {
        PlanGenerateRequest request = request();
        List<AreaBasedItem> local = List.of(
                candidate("lodging-local", "숙소", "32", "AC01"),
                candidate("attraction-local", "관광지", "12", "NA"),
                candidate("restaurant-local", "식당", "39", "FD01"),
                candidate("cafe-local", "카페", "39", "FD05"));
        java.util.ArrayList<AreaBasedItem> chungnam = new java.util.ArrayList<>();
        add(chungnam, "lodging", 2, "32", "AC01");
        add(chungnam, "attraction", 5, "12", "NA");
        add(chungnam, "restaurant", 4, "39", "FD01");
        add(chungnam, "cafe", 4, "39", "FD05");
        when(tourismApiClient.fetchAreaBasedContent("44", "460", 1000)).thenReturn(local);
        when(tourismApiClient.fetchAreaBasedContent("44", null, 1000)).thenReturn(chungnam);
        when(detailService.getAiDetail(anyString(), eq(LocalDate.of(2026, 8, 1))))
                .thenAnswer(invocation -> detail(invocation.getArgument(0), invocation.getArgument(0)));

        var result = service.find(request, List.of(detail("126508", "현충사")));

        assertThat(result).hasSize(15);
        assertThat(result.subList(0, 2)).extracting(item -> item.getCommon().getContentId())
                .contains("lodging-local");
        assertThat(result.subList(11, 15)).extracting(item -> item.getCommon().getContentId())
                .contains("cafe-local");
    }

    private PlanGenerateRequest request() {
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setVisitMonth("202608");
        request.setStayDays(1);
        request.setParticipantCount(20);
        request.setSelectedContentIds(List.of("126508"));
        return request;
    }

    private TourismContentDetailResponse detail(String id, String title) {
        return TourismContentDetailResponse.builder().common(TourismCommonDetailDto.builder()
                .contentId(id).title(title).contentTypeId("12").lDongSignguCd("460")
                .mapx(java.math.BigDecimal.valueOf(126.9))
                .mapy(java.math.BigDecimal.valueOf(36.8)).build()).build();
    }

    private AreaBasedItem candidate(String id, String title) {
        return candidate(id, title, "12", "NA");
    }

    private AreaBasedItem candidate(String id, String title, String contentTypeId, String middleCategory) {
        AreaBasedItem item = new AreaBasedItem();
        item.setContentid(id);
        item.setTitle(title);
        item.setContenttypeid(contentTypeId);
        item.setLclsSystm2(middleCategory);
        item.setMapx("126.9");
        item.setMapy("36.8");
        return item;
    }

    private void add(List<AreaBasedItem> items, String prefix, int count, String type, String category) {
        for (int index = 1; index <= count; index++)
            items.add(candidate(prefix + "-" + index, prefix, type, category));
    }
}
