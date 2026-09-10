package com.wordiga.plan.service;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.member.Member;
import com.wordiga.member.OAuthProvider;
import com.wordiga.member.repository.MemberRepository;
import com.wordiga.plan.Plan;
import com.wordiga.plan.dto.*;
import com.wordiga.plan.dto.ai.AiPlanResponse;
import com.wordiga.plan.repository.PlanRepository;
import com.wordiga.support.PostgresIntegrationTest;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import com.wordiga.tourism.domain.TourismContentSnapshotRepository;
import com.wordiga.tourism.service.TourismContentDetailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PlanServiceIntegrationTest extends PostgresIntegrationTest {
    @Autowired
    PlanService planService;
    @Autowired
    PlanRepository planRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    PlanWriter planWriter;
    @Autowired
    TourismContentSnapshotRepository snapshotRepository;
    @MockitoBean
    TourismContentDetailService tourismContentDetailService;

    @AfterEach
    void cleanUp() {
        planRepository.deleteAll();
        snapshotRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void queriesUpdatesContentsAndDeletesOwnedPlanInPostgres() {
        Member member = memberRepository.save(Member.create("plan@test.com", "테스터", OAuthProvider.GOOGLE, "plan", null));
        Plan plan = planRepository.save(Plan.create(member, "충남 여행", LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 21), 2));
        when(tourismContentDetailService.getCommonDetail("126508")).thenReturn(content("126508"));
        PlanContentsUpdateRequest contents = new PlanContentsUpdateRequest();
        contents.setDays(List.of(day(1, "2026-08-20", "126508"), day(2, "2026-08-21", "126508-2")));
        when(tourismContentDetailService.getCommonDetail("126508-2")).thenReturn(content("126508-2"));

        assertThat(planService.getPlans(member.getId(), 0, 20, PlanSort.LATEST).getItems()).hasSize(1);
        assertThat(planService.updateContents(member.getId(), plan.getId(), contents).getDays()).hasSize(2);
        PlanUpdateRequest update = new PlanUpdateRequest();
        update.setTitle("수정 여행");
        update.setParticipantCount(4);
        assertThat(planService.updatePlan(member.getId(), plan.getId(), update).getParticipantCount()).isEqualTo(4);
        assertThat(planService.getPlan(member.getId(), plan.getId()).getDays()).hasSize(2);

        planService.deletePlan(member.getId(), plan.getId());
        assertThat(planRepository.findById(plan.getId())).isEmpty();
    }

    @Test
    void storesCompleteAiScheduleInOneTransaction() {
        Member member = memberRepository.save(Member.create("generated@test.com", "생성", OAuthProvider.GOOGLE, "generated", null));
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setTitle("AI 일정");
        request.setStartDate(LocalDate.of(2026, 8, 20));
        request.setEndDate(request.getStartDate());
        request.setParticipantCount(2);
        AiPlanResponse.Content content = new AiPlanResponse.Content();
        content.setSequence(1);
        content.setContentId("126508");
        content.setTitle("현충사");
        content.setTravelTimeMinutes(15);
        AiPlanResponse.Day day = new AiPlanResponse.Day();
        day.setDayNumber(1);
        day.setDate(request.getStartDate());
        day.setContents(List.of(content));
        AiPlanResponse ai = new AiPlanResponse();
        ai.setScheduleId(1L);
        ai.setDays(List.of(day));
        AiPlanResponse.EstimatedCost budget = new AiPlanResponse.EstimatedCost();
        budget.setTotalAmount(20_000L);
        budget.setPerPersonAmount(10_000L);
        budget.setCurrency("KRW");
        budget.setBreakdown(java.util.Map.of("food", 20_000L));
        ai.setEstimatedCost(budget);
        snapshotRepository.save(snapshot("126508"));

        PlanDetailResponse saved = planWriter.saveGenerated(member.getId(), request, ai, "아산시");

        assertThat(saved.getScheduleId()).isEqualTo(1L);
        assertThat(saved.getDays().getFirst().getContents().getFirst().getTravelTimeMinutes()).isEqualTo(15);
        assertThat(saved.getEstimatedCost().toString()).contains("food");
        assertThat(planRepository.findById(saved.getPlanId())).isPresent();
    }

    @Test
    void assignsRegionalDateTitleAndSuffixAndIncludesScheduleIdInList() {
        Member member = memberRepository.save(Member.create("title@test.com", "제목", OAuthProvider.GOOGLE, "title", null));
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setStartDate(LocalDate.of(2026, 8, 20));
        request.setEndDate(request.getStartDate());
        request.setParticipantCount(2);
        AiPlanResponse ai = new AiPlanResponse();
        ai.setScheduleId(1L);
        ai.setDays(List.of());

        PlanDetailResponse first = planWriter.saveGenerated(member.getId(), request, ai, "아산시");
        ai.setScheduleId(2L);
        PlanDetailResponse second = planWriter.saveGenerated(member.getId(), request, ai, "아산시");

        assertThat(first.getTitle()).isEqualTo("아산시 0820");
        assertThat(second.getTitle()).isEqualTo("아산시 0820 1");
        assertThat(planService.getPlans(member.getId(), 0, 20, PlanSort.LATEST).getItems())
                .extracting(PlanSummaryResponse::getScheduleId).containsExactly(2L, 1L);
    }

    private ContentDetailDto content(String id) {
        ContentDetailDto c = new ContentDetailDto();
        c.setContentid(id);
        c.setTitle("현충사");
        c.setContenttypeid("12");
        c.setFirstimage("https://example.com/image.jpg");
        c.setAddr1("충청남도 아산시");
        return c;
    }

    private TourismContentSnapshot snapshot(String id) {
        return TourismContentSnapshot.builder().contentId(id).contentTypeId("12").title("현충사")
                .updatedAt(java.time.LocalDateTime.now()).build();
    }

    private PlanContentsUpdateRequest.Day day(int number, String date, String id) {
        PlanContentsUpdateRequest.Day d = new PlanContentsUpdateRequest.Day();
        d.setDayNumber(number);
        d.setContentIds(List.of(id));
        return d;
    }
}
