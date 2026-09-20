package com.wordiga.plan.service;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.plan.Plan;
import com.wordiga.plan.CostSource;
import com.wordiga.plan.CostUnit;
import com.wordiga.plan.PlanContent;
import com.wordiga.plan.dto.PlanContentsUpdateRequest;
import com.wordiga.plan.dto.PlanDetailResponse;
import com.wordiga.plan.dto.PlanSort;
import com.wordiga.plan.dto.PlanUpdateRequest;
import com.wordiga.plan.repository.PlanRepository;
import com.wordiga.proposal.repository.ProposalRepository;
import com.wordiga.proposal.service.ProposalStorage;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import com.wordiga.tourism.domain.TourismContentSnapshotRepository;
import com.wordiga.tourism.service.TourismContentDetailService;
import com.wordiga.tourism.service.TourismContentSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.time.LocalTime;
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
    @Mock
    TourismContentSnapshotRepository snapshotRepository;
    @Mock
    TourismContentSnapshotService snapshotService;
    @Mock
    ProposalRepository proposalRepository;
    @Mock
    ProposalStorage storage;
    PlanService service;
    Plan plan;

    @BeforeEach
    void setUp() {
        service = new PlanService(planRepository, tourismContentDetailService, snapshotRepository, snapshotService,
                proposalRepository, storage);
        plan = Plan.create(null, "충남 여행", LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21), 2);
    }

    @Test
    void listsOnlyMemberPlansAndUpdatesBasicInformation() {
        when(planRepository.findByMemberId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(plan)));
        var list = service.getPlans(1L, 0, 20, PlanSort.LATEST);
        assertThat(list.getItems()).hasSize(1);
        assertThat(list.getTotalCount()).isEqualTo(1);
        assertThat(list.getTotalPages()).isEqualTo(1);
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        PlanUpdateRequest request = new PlanUpdateRequest();
        request.setTitle(" 수정 일정 ");
        request.setParticipantCount(4);
        assertThat(service.updatePlan(1L, 9L, request).getTitle()).isEqualTo("수정 일정");
    }

    @Test
    void recalculatesTotalBudgetAndKeepsPerPersonBreakdownWhenParticipantCountChanges() {
        plan.applyAiResult(1L, 20_000L, 10_000L, java.util.Map.of("food", 20_000L));
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        PlanUpdateRequest request = new PlanUpdateRequest();
        request.setParticipantCount(4);

        PlanDetailResponse result = service.updatePlan(1L, 9L, request);

        assertThat(result.getScheduleId()).isEqualTo(1L);
        assertThat(result.getEstimatedBudget().totalAmount()).isEqualTo(40_000L);
        assertThat(result.getEstimatedBudget().breakdown()).containsEntry("food", 20_000L);
    }

    @Test
    void storesOnlyCompleteStructuredBudgetItems() {
        java.util.Map<String, Long> breakdown = new java.util.LinkedHashMap<>();
        breakdown.put("food", 20_000L);
        breakdown.put("", 5_000L);
        breakdown.put("activity", null);

        plan.applyAiResult(1L, 20_000L, 10_000L, breakdown);
        PlanDetailResponse result = PlanDetailResponse.from(plan);

        assertThat(result.getEstimatedBudget().currency()).isEqualTo("KRW");
        assertThat(result.getEstimatedBudget().breakdown()).containsExactly(entry("food", 20_000L));
    }

    @Test
    void returnsStoredThumbnailForEachPlanContent() {
        TourismContentSnapshot content = TourismContentSnapshot.builder()
                .contentId("126508").contentTypeId("12").title("현충사")
                .firstimage("https://image.example/126508.jpg").build();
        plan.addContent(com.wordiga.plan.PlanContent.create(plan, 1, 1, content));
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));

        PlanDetailResponse result = service.getPlan(1L, 9L);

        assertThat(result.getDays().getFirst().getContents().getFirst().getThumbnailUrl())
                .isEqualTo("https://image.example/126508.jpg");
    }

    @Test
    void replacesContentsInDayAndAllowsRepeatedContent() {
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        when(tourismContentDetailService.getCommonDetail(anyString())).thenAnswer(invocation -> {
            ContentDetailDto content = new ContentDetailDto();
            content.setContentid(invocation.getArgument(0));
            content.setTitle("현충사");
            content.setContenttypeid("12");
            return content;
        });
        when(snapshotRepository.save(any(TourismContentSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PlanContentsUpdateRequest request = new PlanContentsUpdateRequest();
        request.setDays(List.of(day(1, "2026-08-20", "A", "A"), day(2, "2026-08-21", "A")));

        var result = service.updateContents(1L, 9L, request);

        assertThat(result.getDays()).hasSize(2);
        assertThat(result.getDays().getFirst().getContents()).extracting(PlanDetailResponse.Content::getContentId)
                .containsExactly("A", "A");
        assertThat(result.getDays()).hasSize(2);
        assertThat(result.getDays().get(1).getContents()).extracting(PlanDetailResponse.Content::getContentId)
                .containsExactly("A");
        assertThat(result.getDays().getFirst().getContents().getFirst().getCost().perPersonShare())
                .isEqualTo(10_000L);
        assertThat(result.getEstimatedBudget().breakdown()).containsEntry("관광지", 30_000L);
    }

    @Test
    void preservesStoredCostWhenContentsAreReordered() {
        TourismContentSnapshot snapshot = TourismContentSnapshot.builder()
                .contentId("A").contentTypeId("14").title("박물관").build();
        PlanContent stored = PlanContent.create(plan, 1, 1, snapshot);
        stored.updateCost(7_000L, CostUnit.PERSON, 2, 14_000L, 7_000L, CostSource.TOUR_API);
        plan.addContent(stored);
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        when(tourismContentDetailService.getCommonDetail("A")).thenReturn(content("A", "14"));
        when(snapshotRepository.save(any(TourismContentSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PlanContentsUpdateRequest request = new PlanContentsUpdateRequest();
        request.setDays(List.of(day(1, "2026-08-20"), day(2, "2026-08-21", "A")));

        var result = service.updateContents(1L, 9L, request);

        var cost = result.getDays().getFirst().getContents().getFirst().getCost();
        assertThat(cost.unitAmount()).isEqualTo(7_000L);
        assertThat(cost.totalAmount()).isEqualTo(14_000L);
        assertThat(cost.source()).isEqualTo(CostSource.TOUR_API);
        assertThat(result.getEstimatedBudget().breakdown()).containsExactly(entry("문화시설", 7_000L));
    }

    @Test
    void clearsOnlyTravelDetailsWhosePreviousContentChanged() {
        PlanContent first = routeContent("A", 1, 1, 0, 0);
        PlanContent unchanged = routeContent("B", 2, 1, 12, 6_000);
        PlanContent affected = routeContent("C", 3, 1, 18, 9_000);
        plan.addContent(first);
        plan.addContent(unchanged);
        plan.addContent(affected);
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        when(tourismContentDetailService.getCommonDetail(anyString()))
                .thenAnswer(invocation -> content(invocation.getArgument(0), "12"));
        when(snapshotRepository.save(any(TourismContentSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PlanContentsUpdateRequest request = new PlanContentsUpdateRequest();
        request.setDays(List.of(day(1, "2026-08-20", "A", "B", "D", "C"),
                day(2, "2026-08-21")));

        var result = service.updateContents(1L, 9L, request);

        var updated = result.getDays().getFirst().getContents();
        assertThat(updated.get(0).getTravelTimeMinutes()).isNull();
        assertThat(updated.get(1).getTravelTimeMinutes()).isEqualTo(12);
        assertThat(updated.get(1).getTravelDistanceMeters()).isEqualTo(6_000);
        assertThat(updated.get(2).getTravelTimeMinutes()).isNull();
        assertThat(updated.get(3).getTravelTimeMinutes()).isNull();
        assertThat(updated.get(3).getTravelDistanceMeters()).isNull();
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
    void rejectsDifferentLodgingsAcrossEditedDays() {
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        when(tourismContentDetailService.getCommonDetail("hotel-a")).thenReturn(content("hotel-a", "32"));
        when(tourismContentDetailService.getCommonDetail("hotel-b")).thenReturn(content("hotel-b", "32"));
        when(snapshotRepository.save(any(TourismContentSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PlanContentsUpdateRequest request = new PlanContentsUpdateRequest();
        request.setDays(List.of(day(1, "2026-08-20", "hotel-a"), day(2, "2026-08-21", "hotel-b")));

        assertThatThrownBy(() -> service.updateContents(1L, 9L, request))
                .hasMessageContaining("하나의 숙소");
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

    private ContentDetailDto content(String id, String type) {
        ContentDetailDto content = new ContentDetailDto();
        content.setContentid(id);
        content.setContenttypeid(type);
        content.setTitle(id);
        return content;
    }

    private PlanContent routeContent(String id, int sequence, int day, int travelMinutes, int travelMeters) {
        TourismContentSnapshot snapshot = TourismContentSnapshot.builder()
                .contentId(id).contentTypeId("12").title(id).build();
        PlanContent content = PlanContent.create(plan, sequence, day, snapshot);
        content.updateAiDetails(null, 90, LocalTime.of(10, 0), LocalTime.of(11, 30),
                travelMinutes, travelMeters, 0L);
        return content;
    }
}
