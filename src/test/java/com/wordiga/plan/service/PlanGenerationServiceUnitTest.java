package com.wordiga.plan.service;

import com.wordiga.global.client.AiServerClient;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.plan.dto.PlanDetailResponse;
import com.wordiga.plan.dto.PlanGenerateRequest;
import com.wordiga.plan.dto.ai.AiPlanRequest;
import com.wordiga.plan.dto.ai.AiPlanResponse;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.tourism.service.RegionalContentService;
import com.wordiga.tourism.service.TourismContentDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanGenerationServiceUnitTest {
    @Mock
    TourismContentDetailService tourismContentDetailService;
    @Mock
    RegionalContentService regionalContentService;
    @Mock
    TourismApiClient tourismApiClient;
    @Mock
    AiServerClient aiServerClient;
    @Mock
    PlanWriter planWriter;
    PlanGenerationService service;

    @BeforeEach
    void setUp() {
        lenient().when(regionalContentService.find(any(), anyList())).thenReturn(List.of());
        service = new PlanGenerationService(tourismContentDetailService, regionalContentService, tourismApiClient, aiServerClient, planWriter);
    }

    @Test
    void callsAiOutsideWriterAndPersistsValidatedResponse() {
        PlanGenerateRequest request = request("126508");
        AiPlanResponse response = response("126508");
        when(tourismContentDetailService.getAiDetail(eq("126508"), any())).thenReturn(detail());
        when(tourismApiClient.fetchClassificationNames("AC", "AC01", null))
                .thenReturn(List.of("숙박", "호텔"));
        when(aiServerClient.generatePlan(any())).thenReturn(response);
        when(planWriter.saveGenerated(eq(1L), eq(request), eq(response), anyList(), isNull()))
                .thenReturn(PlanDetailResponse.builder().planId(9L).build());

        assertThat(service.generate(1L, request).getPlanId()).isEqualTo(9L);
        var captor = org.mockito.ArgumentCaptor.forClass(AiPlanRequest.class);
        verify(aiServerClient).generatePlan(captor.capture());
        assertThat(captor.getValue().savedContents().get(0).tags()).containsExactly("숙박", "호텔");
        verify(planWriter).saveGenerated(eq(1L), eq(request), eq(response), anyList(), isNull());
    }

    @Test
    void sendsKoreanClassificationNamesAndFallsBackToNullOnFailure() {
        PlanGenerateRequest request = request("126508");
        AiPlanResponse response = response("126508");
        when(tourismContentDetailService.getAiDetail(eq("126508"), any())).thenReturn(detail());
        when(tourismApiClient.fetchClassificationNames("AC", "AC01", null))
                .thenThrow(new RuntimeException("분류 API 장애"));
        when(aiServerClient.generatePlan(any())).thenReturn(response);

        service.generate(1L, request);

        var captor = org.mockito.ArgumentCaptor.forClass(AiPlanRequest.class);
        verify(aiServerClient).generatePlan(captor.capture());
        assertThat(captor.getValue().savedContents().get(0).tags()).isEmpty();
    }

    @Test
    void rejectsDuplicateSelectionAndInvalidAiContent() {
        assertThatThrownBy(() -> service.generate(1L, request("A", "A"))).hasMessageContaining("400");
        PlanGenerateRequest request = request("A");
        AiPlanResponse response = response("B");
        when(tourismContentDetailService.getAiDetail(eq("A"), any())).thenReturn(detail());
        when(aiServerClient.generatePlan(any())).thenReturn(response);
        assertThatThrownBy(() -> service.generate(1L, request)).hasMessageContaining("502");
        verifyNoInteractions(planWriter);
    }

    @Test
    void sendsSelectedAndRegionalContentSeparately() {
        PlanGenerateRequest request = request("126508");
        TourismContentDetailResponse regional = detail("regional-1", "외암민속마을");
        when(tourismContentDetailService.getAiDetail(eq("126508"), any())).thenReturn(detail());
        when(regionalContentService.find(eq(request), anyList())).thenReturn(List.of(regional));
        when(aiServerClient.generatePlan(any())).thenReturn(response("126508", "regional-1"));

        service.generate(1L, request);

        var captor = org.mockito.ArgumentCaptor.forClass(AiPlanRequest.class);
        verify(aiServerClient).generatePlan(captor.capture());
        assertThat(captor.getValue().savedContents()).extracting(AiPlanRequest.Content::contentId)
                .containsExactly("126508");
        assertThat(captor.getValue().regionalContents()).extracting(AiPlanRequest.Content::contentId)
                .containsExactly("regional-1");
    }

    @Test
    void logsSelectedContentOmissionWithoutBlockingTestGeneration() {
        PlanGenerateRequest request = request("126508");
        TourismContentDetailResponse regional = detail("regional-1", "외암민속마을");
        when(tourismContentDetailService.getAiDetail(eq("126508"), any())).thenReturn(detail());
        when(regionalContentService.find(eq(request), anyList())).thenReturn(List.of(regional));
        when(aiServerClient.generatePlan(any())).thenReturn(response("regional-1"));

        service.generate(1L, request);

        verify(planWriter).saveGenerated(eq(1L), eq(request), any(), anyList(), isNull());
    }

    @Test
    void rejectsInvalidVisitMonth() {
        PlanGenerateRequest request = request("A");
        request.setVisitMonth("2026-13");

        assertThatThrownBy(() -> service.generate(1L, request)).hasMessageContaining("YYYY-MM");
        verifyNoInteractions(aiServerClient, planWriter);
    }

    @Test
    void rejectsWrongDayCountButAllowsSameContentOnDifferentDays() {
        PlanGenerateRequest request = request("126508");
        request.setStayDays(2);
        when(tourismContentDetailService.getAiDetail(eq("126508"), any())).thenReturn(detail());
        when(aiServerClient.generatePlan(any())).thenReturn(response("126508"));

        assertThatThrownBy(() -> service.generate(1L, request)).hasMessageContaining("502");

        AiPlanResponse response = new AiPlanResponse();
        response.setScheduleId(1L);
        response.setDays(List.of(day(1, "126508"), day(2, "126508")));
        when(aiServerClient.generatePlan(any())).thenReturn(response);

        service.generate(1L, request);
        verify(planWriter).saveGenerated(eq(1L), eq(request), eq(response), anyList(), isNull());
    }

    @Test
    void normalizesAiSequenceAndDropsDuplicatesOnlyWithinEachDay() {
        PlanGenerateRequest request = request("126508");
        request.setStayDays(2);
        AiPlanResponse response = new AiPlanResponse();
        AiPlanResponse.Day first = day(1, "126508", "126508");
        first.getContents().get(0).setSequence(7);
        first.getContents().get(1).setSequence(9);
        AiPlanResponse.Day second = day(2, "126508");
        second.getContents().get(0).setSequence(4);
        response.setDays(List.of(first, second));
        when(tourismContentDetailService.getAiDetail(eq("126508"), any())).thenReturn(detail());
        when(aiServerClient.generatePlan(any())).thenReturn(response);

        service.generate(1L, request);

        assertThat(first.getContents()).hasSize(1);
        assertThat(first.getContents().getFirst().getSequence()).isEqualTo(1);
        assertThat(second.getContents().getFirst().getSequence()).isEqualTo(1);
        verify(planWriter).saveGenerated(eq(1L), eq(request), eq(response), anyList(), isNull());
    }

    @Test
    void leavesWeatherOutOfPlanGenerationContentCalls() {
        PlanGenerateRequest request = request("A", "B");
        LocalDate targetDate = visitDate();

        when(tourismContentDetailService.getAiDetail("A", targetDate))
                .thenReturn(detail("A", "A 관광지"));
        when(tourismContentDetailService.getAiDetail("B", targetDate))
                .thenReturn(detail("B", "B 관광지"));
        when(aiServerClient.generatePlan(any())).thenReturn(response("A", "B"));

        service.generate(1L, request);

        verify(tourismContentDetailService).getAiDetail("A", targetDate);
        verify(tourismContentDetailService).getAiDetail("B", targetDate);
    }

    private PlanGenerateRequest request(String... contentIds) {
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setTitle("테스트 일정");
        request.setVisitMonth(visitMonth());
        request.setStayDays(1);
        request.setParticipantCount(10);
        request.setSelectedContentIds(new ArrayList<>(List.of(contentIds)));
        return request;
    }

    private AiPlanResponse response(String id) {
        return response(new String[]{id});
    }

    private AiPlanResponse response(String... ids) {
        java.util.ArrayList<AiPlanResponse.Content> contents = new java.util.ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            AiPlanResponse.Content c = new AiPlanResponse.Content();
            c.setSequence(i + 1);
            c.setContentId(ids[i]);
            c.setTitle(ids[i]);
            contents.add(c);
        }
        AiPlanResponse.Day d = new AiPlanResponse.Day();
        d.setDayNumber(1);
        d.setDate(LocalDate.of(2026, 8, 20));
        d.setContents(contents);
        AiPlanResponse r = new AiPlanResponse();
        r.setScheduleId(1L);
        r.setDays(List.of(d));
        return r;
    }

    private AiPlanResponse.Day day(int dayNumber, String... ids) {
        java.util.ArrayList<AiPlanResponse.Content> contents = new java.util.ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            AiPlanResponse.Content content = new AiPlanResponse.Content();
            content.setSequence(i + 1);
            content.setContentId(ids[i]);
            contents.add(content);
        }
        AiPlanResponse.Day day = new AiPlanResponse.Day();
        day.setDayNumber(dayNumber);
        day.setContents(contents);
        return day;
    }

    private TourismContentDetailResponse detail() {
        return detail("126508", "현충사");
    }

    private TourismContentDetailResponse detail(String id, String title) {
        return TourismContentDetailResponse.builder().common(
                com.wordiga.tourism.dto.detail.TourismCommonDetailDto.builder()
                        .contentId(id).contentTypeId("12").title(title)
                        .mapx(java.math.BigDecimal.valueOf(127.1)).mapy(java.math.BigDecimal.valueOf(36.8))
                        .lDongSignguCd("131")
                        .lclsSystm1("AC").lclsSystm2("AC01").build()).build();
    }

    private String visitMonth() {
        return YearMonth.now().plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private LocalDate visitDate() {
        return YearMonth.now().plusMonths(1).atDay(1);
    }
}
