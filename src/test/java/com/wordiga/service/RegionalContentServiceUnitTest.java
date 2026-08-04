package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.AreaBasedItem;
import com.wordiga.client.dto.AreaBasedResponse;
import com.wordiga.client.dto.KtoApiResponse;
import com.wordiga.client.dto.RelatedTourismItem;
import com.wordiga.dto.plan.PlanGenerateRequest;
import com.wordiga.dto.tourismContent.detail.TourismCommonDetailDto;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
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
    @Mock TourismApiClient tourismApiClient;
    @Mock TourismContentDetailService detailService;
    RegionalContentService service;

    @BeforeEach void setUp() { service = new RegionalContentService(tourismApiClient, detailService); }

    @Test void resolvesRelatedNamesThroughKorServiceAndExcludesSavedContent() {
        PlanGenerateRequest request = request();
        RelatedTourismItem duplicate = related("현충사", "44460", 1);
        RelatedTourismItem candidate = related("외암민속마을", "44460", 2);
        when(tourismApiClient.fetchRelatedTourism(anyString(), eq("44"), eq("44460"), eq("현충사"), eq(5)))
                .thenReturn(List.of(duplicate, candidate));
        when(tourismApiClient.searchContent(eq("현충사"), isNull(), eq("44"), eq("460"), eq(1), eq(10)))
                .thenReturn(searchResult("126508", "현충사"));
        when(tourismApiClient.searchContent(eq("외암민속마을"), isNull(), eq("44"), eq("460"), eq(1), eq(10)))
                .thenReturn(searchResult("regional-1", "외암민속마을"));
        TourismContentDetailResponse enriched = detail("regional-1", "외암민속마을");
        when(detailService.getDetail("regional-1", request.getStartDate(), request.getAgeGroups(), 20))
                .thenReturn(enriched);

        assertThat(service.find(request, List.of(detail("126508", "현충사"))))
                .extracting(item -> item.getCommon().getContentId()).containsExactly("regional-1");
        verify(detailService, never()).getDetail(eq("126508"), any(), any(), anyInt());
    }

    private PlanGenerateRequest request() {
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setStartDate(LocalDate.of(2026, 8, 20)); request.setEndDate(request.getStartDate());
        request.setParticipantCount(20); request.setSelectedContentIds(List.of("126508"));
        return request;
    }

    private TourismContentDetailResponse detail(String id, String title) {
        return TourismContentDetailResponse.builder().common(TourismCommonDetailDto.builder()
                .contentId(id).title(title).lDongSignguCd("460").build()).build();
    }

    private RelatedTourismItem related(String title, String signgu, int rank) {
        RelatedTourismItem item = new RelatedTourismItem(); item.setRlteTatsNm(title);
        item.setRlteRegnCd("44"); item.setRlteSignguCd(signgu); item.setRlteRank(rank); return item;
    }

    private AreaBasedResponse searchResult(String id, String title) {
        AreaBasedItem item = new AreaBasedItem(); item.setContentid(id); item.setTitle(title);
        KtoApiResponse.Items<AreaBasedItem> items = new KtoApiResponse.Items<>(); items.setItem(List.of(item));
        KtoApiResponse.Body<AreaBasedItem> body = new KtoApiResponse.Body<>(); body.setItems(items);
        KtoApiResponse.Response<AreaBasedItem> response = new KtoApiResponse.Response<>(); response.setBody(body);
        AreaBasedResponse result = new AreaBasedResponse(); result.setResponse(response); return result;
    }
}
