package com.wordiga.service;

import com.wordiga.client.AiServerClient;
import com.wordiga.client.TourismApiClient;
import com.wordiga.dto.ai.*;
import com.wordiga.dto.plan.*;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanGenerationServiceUnitTest {
    @Mock TourismContentDetailService tourismContentDetailService;
    @Mock TourismApiClient tourismApiClient;
    @Mock AiServerClient aiServerClient;
    @Mock PlanWriter planWriter;
    PlanGenerationService service;
    @BeforeEach void setUp() { service = new PlanGenerationService(tourismContentDetailService, tourismApiClient, aiServerClient, planWriter); }

    @Test void callsAiOutsideWriterAndPersistsValidatedResponse() {
        PlanGenerateRequest request = request("126508"); AiPlanResponse response = response("126508");
        when(tourismContentDetailService.getDetail(eq("126508"), any(), any(), eq(2))).thenReturn(detail());
        when(tourismApiClient.fetchClassificationNames("AC", "AC01", null))
                .thenReturn(List.of("숙박", "호텔"));
        when(aiServerClient.generatePlan(any())).thenReturn(response);
        when(planWriter.saveGenerated(1L, request, response)).thenReturn(PlanDetailResponse.builder().planId(9L).build());

        assertThat(service.generate(1L, request).getPlanId()).isEqualTo(9L);
        var captor = org.mockito.ArgumentCaptor.forClass(AiPlanRequest.class);
        verify(aiServerClient).generatePlan(captor.capture());
        assertThat(captor.getValue().savedContents().get(0).tags()).containsExactly("숙박", "호텔");
        verify(planWriter).saveGenerated(1L, request, response);
    }

    @Test void sendsKoreanClassificationNamesAndFallsBackToNullOnFailure() {
        PlanGenerateRequest request = request("126508"); AiPlanResponse response = response("126508");
        when(tourismContentDetailService.getDetail(eq("126508"), any(), any(), eq(2))).thenReturn(detail());
        when(tourismApiClient.fetchClassificationNames("AC", "AC01", null))
                .thenThrow(new RuntimeException("분류 API 장애"));
        when(aiServerClient.generatePlan(any())).thenReturn(response);

        service.generate(1L, request);

        var captor = org.mockito.ArgumentCaptor.forClass(AiPlanRequest.class);
        verify(aiServerClient).generatePlan(captor.capture());
        assertThat(captor.getValue().savedContents().get(0).tags()).isNull();
    }

    @Test void rejectsDuplicateSelectionAndInvalidAiContent() {
        assertThatThrownBy(() -> service.generate(1L, request("A", "A"))).hasMessageContaining("400");
        PlanGenerateRequest request = request("A"); AiPlanResponse response = response("B");
        when(tourismContentDetailService.getDetail(eq("A"), any(), any(), eq(2))).thenReturn(detail());
        when(aiServerClient.generatePlan(any())).thenReturn(response);
        assertThatThrownBy(() -> service.generate(1L, request)).hasMessageContaining("502");
        verifyNoInteractions(planWriter);
    }

    @Test void rejectsPlanLongerThanThreeDays() {
        PlanGenerateRequest request = request("A");
        request.setEndDate(request.getStartDate().plusDays(3));

        assertThatThrownBy(() -> service.generate(1L, request)).hasMessageContaining("1~3일");
        verifyNoInteractions(aiServerClient, planWriter);
    }

    private PlanGenerateRequest request(String... ids) {
        PlanGenerateRequest r = new PlanGenerateRequest(); r.setStartDate(LocalDate.of(2026, 8, 20));
        r.setEndDate(r.getStartDate()); r.setParticipantCount(2); r.setSelectedContentIds(List.of(ids)); return r;
    }
    private AiPlanResponse response(String id) {
        AiPlanResponse.Content c = new AiPlanResponse.Content(); c.setSequence(1); c.setContentId(id); c.setTitle("현충사");
        AiPlanResponse.Day d = new AiPlanResponse.Day(); d.setDayNumber(1); d.setDate(LocalDate.of(2026, 8, 20)); d.setContents(List.of(c));
        AiPlanResponse r = new AiPlanResponse(); r.setScheduleId("schedule-1"); r.setDays(List.of(d)); return r;
    }
    private TourismContentDetailResponse detail() {
        return TourismContentDetailResponse.builder().common(
                com.wordiga.dto.tourismContent.detail.TourismCommonDetailDto.builder()
                        .contentId("126508").contentTypeId("12").title("현충사")
                        .lclsSystm1("AC").lclsSystm2("AC01").build()).build();
    }
}
