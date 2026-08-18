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
        when(tourismApiClient.fetchAreaBasedContent("44", "460", 100))
                .thenReturn(List.of(duplicate, candidate));
        TourismContentDetailResponse enriched = detail("regional-1", "외암민속마을");
        when(detailService.getAiDetail("regional-1", request.getStartDate(), false))
                .thenReturn(enriched);

        assertThat(service.find(request, List.of(detail("126508", "현충사"))))
                .extracting(item -> item.getCommon().getContentId()).containsExactly("regional-1");
        verify(detailService, never()).getAiDetail(eq("126508"), any(), anyBoolean());
    }

    private PlanGenerateRequest request() {
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setStartDate(LocalDate.of(2026, 8, 20));
        request.setEndDate(request.getStartDate());
        request.setParticipantCount(20);
        request.setSelectedContentIds(List.of("126508"));
        return request;
    }

    private TourismContentDetailResponse detail(String id, String title) {
        return TourismContentDetailResponse.builder().common(TourismCommonDetailDto.builder()
                .contentId(id).title(title).lDongSignguCd("460")
                .mapx(java.math.BigDecimal.valueOf(126.9))
                .mapy(java.math.BigDecimal.valueOf(36.8)).build()).build();
    }

    private AreaBasedItem candidate(String id, String title) {
        AreaBasedItem item = new AreaBasedItem();
        item.setContentid(id);
        item.setTitle(title);
        item.setContenttypeid("12");
        item.setMapx("126.9");
        item.setMapy("36.8");
        return item;
    }
}
