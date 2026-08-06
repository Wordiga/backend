package com.wordiga.service;

import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.plan.PlanContentsUpdateRequest;
import com.wordiga.dto.plan.PlanDetailResponse;
import com.wordiga.dto.plan.PlanSort;
import com.wordiga.dto.plan.PlanUpdateRequest;
import com.wordiga.plan.Plan;
import com.wordiga.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceUnitTest {
    @Mock
    PlanRepository planRepository;
    @Mock
    TourismContentDetailService tourismContentDetailService;
    PlanService service;
    Plan plan;

    @BeforeEach
    void setUp() {
        service = new PlanService(planRepository, tourismContentDetailService);
        plan = Plan.create(null, "충남 여행", LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21), 2, null);
    }

    @Test
    void listsOnlyMemberPlansAndUpdatesBasicInformation() {
        when(planRepository.findByMemberId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(plan)));
        assertThat(service.getPlans(1L, 0, 20, PlanSort.LATEST).getItems()).hasSize(1);
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        PlanUpdateRequest request = new PlanUpdateRequest();
        request.setTitle(" 수정 일정 ");
        request.setParticipantCount(4);
        assertThat(service.updatePlan(1L, 9L, request).getTitle()).isEqualTo("수정 일정");
    }

    @Test
    void recalculatesTotalBudgetAndKeepsScheduleWhenParticipantCountChanges() {
        plan.applyAiResult("schedule-1", 20_000L, 10_000L, "KRW", java.util.Map.of("food", 20_000L));
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        PlanUpdateRequest request = new PlanUpdateRequest();
        request.setParticipantCount(4);

        PlanDetailResponse result = service.updatePlan(1L, 9L, request);

        assertThat(result.getScheduleId()).isEqualTo("schedule-1");
        assertThat(result.getEstimatedBudget().totalAmount()).isEqualTo(40_000L);
    }

    @Test
    void storesOnlyCompleteStructuredBudgetItems() {
        java.util.Map<String, Long> breakdown = new java.util.LinkedHashMap<>();
        breakdown.put("food", 20_000L);
        breakdown.put("", 5_000L);
        breakdown.put("activity", null);

        plan.applyAiResult("schedule-1", 20_000L, 10_000L, "KRW", breakdown);
        PlanDetailResponse result = PlanDetailResponse.from(plan);

        assertThat(result.getEstimatedBudget().currency()).isEqualTo("KRW");
        assertThat(result.getEstimatedBudget().breakdown()).containsExactly(entry("food", 20_000L));
    }

    @Test
    void replacesContentsInDayAndRequestOrder() {
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        ContentDetailDto content = new ContentDetailDto();
        content.setTitle("현충사");
        content.setContenttypeid("12");
        when(tourismContentDetailService.getCommonDetail(anyString())).thenReturn(content);
        PlanContentsUpdateRequest request = new PlanContentsUpdateRequest();
        request.setDays(List.of(day(1, "2026-08-20", "A", "B"), day(2, "2026-08-21", "C")));

        var result = service.updateContents(1L, 9L, request);

        assertThat(result.getDays()).hasSize(2);
        assertThat(result.getDays().getFirst().getContents()).extracting(PlanDetailResponse.Content::getContentId)
                .containsExactly("A", "B");
        assertThat(result.getDays()).hasSize(2);
    }

    @Test
    void rejectsOtherMembersPlanAndInvalidDaySequence() {
        assertThatThrownBy(() -> service.getPlan(1L, 9L)).hasMessageContaining("404");
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        PlanContentsUpdateRequest request = new PlanContentsUpdateRequest();
        request.setDays(List.of(day(2, "2026-08-20", "A"), day(1, "2026-08-21", "B")));
        assertThatThrownBy(() -> service.updateContents(1L, 9L, request)).hasMessageContaining("400");
    }

    @Test
    void deleteIsIdempotentOnlyForOwnedExistingPlan() {
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        service.deletePlan(1L, 9L);
        verify(planRepository).delete(plan);
    }

    private PlanContentsUpdateRequest.Day day(int number, String date, String... ids) {
        PlanContentsUpdateRequest.Day day = new PlanContentsUpdateRequest.Day();
        day.setDayNumber(number);
        day.setContentIds(List.of(ids));
        return day;
    }
}
