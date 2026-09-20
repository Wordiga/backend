package com.wordiga.plan.service;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.member.Member;
import com.wordiga.member.OAuthProvider;
import com.wordiga.member.repository.MemberRepository;
import com.wordiga.plan.Plan;
import com.wordiga.plan.CostSource;
import com.wordiga.plan.CostUnit;
import com.wordiga.plan.PlanContent;
import com.wordiga.plan.dto.*;
import com.wordiga.plan.dto.ai.AiPlanResponse;
import com.wordiga.plan.repository.PlanRepository;
import com.wordiga.support.PostgresIntegrationTest;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import com.wordiga.tourism.domain.TourismContentSnapshotRepository;
import com.wordiga.tourism.dto.detail.TourismCommonDetailDto;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.tourism.service.TourismContentDetailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.math.BigDecimal;
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
    @Autowired
    JdbcTemplate jdbcTemplate;
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
        request.setVisitMonth("2026-08");
        request.setStayDays(1);
        request.setParticipantCount(2);
        AiPlanResponse.Content content = new AiPlanResponse.Content();
        content.setSequence(1);
        content.setContentId("126508");
        content.setTitle("현충사");
        content.setTravelTimeMinutes(15);
        AiPlanResponse.Day day = new AiPlanResponse.Day();
        day.setDayNumber(1);
        day.setDate(LocalDate.of(2026, 8, 1));
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
        assertThat(saved.getEstimatedBudget().toString()).contains("food");
        assertThat(planRepository.findById(saved.getPlanId())).isPresent();
    }

    @Test
    void preservesMissingAiTravelDistanceAndCalculatesPerPersonBudgetBreakdown() {
        Member member = memberRepository.save(Member.create("calculation@test.com", "계산", OAuthProvider.GOOGLE,
                "calculation", null));
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setVisitMonth("2026-08");
        request.setStayDays(1);
        request.setParticipantCount(10);

        AiPlanResponse.Content first = aiContent(1, "culture");
        AiPlanResponse.Content second = aiContent(2, "restaurant");
        AiPlanResponse.Day day = new AiPlanResponse.Day();
        day.setDayNumber(1);
        day.setDate(LocalDate.of(2026, 8, 1));
        day.setContents(List.of(first, second));
        AiPlanResponse ai = new AiPlanResponse();
        ai.setScheduleId(3L);
        ai.setDays(List.of(day));

        snapshotRepository.save(snapshot("culture", "14", "127.1000", "36.8000"));
        snapshotRepository.save(snapshot("restaurant", "39", "127.1100", "36.8100"));
        List<TourismContentDetailResponse> details = List.of(detail("culture", "14"), detail("restaurant", "39"));

        PlanDetailResponse saved = planWriter.saveGenerated(member.getId(), request, ai, details, "천안시");

        assertThat(saved.getDays().getFirst().getContents()).extracting(PlanDetailResponse.Content::getTravelDistanceMeters)
                .containsOnlyNulls();
        assertThat(saved.getEstimatedBudget().totalAmount()).isEqualTo(250_000);
        assertThat(saved.getEstimatedBudget().perPersonAmount()).isEqualTo(25_000);
        assertThat(saved.getEstimatedBudget().breakdown())
                .containsEntry("문화시설", 10_000L).containsEntry("음식점", 15_000L);
        assertThat(saved.getDays().getFirst().getContents()).allSatisfy(content -> {
            assertThat(content.getCost()).isNotNull();
            assertThat(content.getCost().source()).isEqualTo(com.wordiga.plan.CostSource.DEFAULT);
            assertThat(content.getCost().totalAmount()).isEqualTo(content.getEstimatedCost());
        });
    }

    @Test
    void updatesBudgetBreakdownsWithoutViolatingCategoryUniqueConstraint() {
        Member member = memberRepository.save(Member.create("budget-update@test.com", "예산", OAuthProvider.GOOGLE,
                "budget-update", null));
        Plan plan = Plan.create(member, "예산 수정", LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 20), 10);
        TourismContentSnapshot restaurant = snapshotRepository.save(snapshot("restaurant", "39", null, null));
        TourismContentSnapshot culture = snapshotRepository.save(snapshot("culture", "14", null, null));
        PlanContent restaurantContent = PlanContent.create(plan, 1, 1, restaurant);
        restaurantContent.updateCost(15_000L, CostUnit.PERSON, 10, 150_000L, 15_000L, CostSource.DEFAULT);
        PlanContent cultureContent = PlanContent.create(plan, 2, 1, culture);
        cultureContent.updateCost(10_000L, CostUnit.PERSON, 10, 100_000L, 10_000L, CostSource.DEFAULT);
        plan.addContent(restaurantContent);
        plan.addContent(cultureContent);
        plan.applyAiResult(1L, 250_000L, 25_000L,
                new java.util.LinkedHashMap<>(java.util.Map.of("음식점", 15_000L, "문화시설", 10_000L)));
        plan = planRepository.saveAndFlush(plan);

        when(tourismContentDetailService.getCommonDetail("restaurant")).thenReturn(content("restaurant", "39"));
        when(tourismContentDetailService.getCommonDetail("tourist")).thenReturn(content("tourist", "12"));
        PlanContentsUpdateRequest update = new PlanContentsUpdateRequest();
        update.setDays(List.of(day(1, "2026-08-20", "restaurant", "tourist")));

        PlanDetailResponse result = planService.updateContents(member.getId(), plan.getId(), update);
        planRepository.flush();

        assertThat(result.getEstimatedBudget().breakdown())
                .containsExactlyInAnyOrderEntriesOf(java.util.Map.of("음식점", 15_000L, "관광지", 10_000L));
        assertThat(jdbcTemplate.queryForList(
                        "select category from plan_budget_breakdowns where plan_id = ?", String.class, plan.getId()))
                .containsExactlyInAnyOrder("음식점", "관광지");
    }

    @Test
    void assignsRegionalDateTitleAndSuffixAndIncludesScheduleIdInList() {
        Member member = memberRepository.save(Member.create("title@test.com", "제목", OAuthProvider.GOOGLE, "title", null));
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setVisitMonth("2026-08");
        request.setStayDays(1);
        request.setParticipantCount(2);
        AiPlanResponse ai = new AiPlanResponse();
        ai.setScheduleId(1L);
        ai.setDays(List.of());

        PlanDetailResponse first = planWriter.saveGenerated(member.getId(), request, ai, "아산시");
        ai.setScheduleId(2L);
        PlanDetailResponse second = planWriter.saveGenerated(member.getId(), request, ai, "아산시");

        assertThat(first.getTitle()).isEqualTo("아산시 26년 08월");
        assertThat(second.getTitle()).isEqualTo("아산시 26년 08월 1");
        assertThat(planService.getPlans(member.getId(), 0, 20, PlanSort.LATEST).getItems())
                .extracting(PlanSummaryResponse::getScheduleId).containsExactly(2L, 1L);
    }

    private ContentDetailDto content(String id) {
        return content(id, "12");
    }

    private ContentDetailDto content(String id, String type) {
        ContentDetailDto c = new ContentDetailDto();
        c.setContentid(id);
        c.setTitle("현충사");
        c.setContenttypeid(type);
        c.setFirstimage("https://example.com/image.jpg");
        c.setAddr1("충청남도 아산시");
        return c;
    }

    private TourismContentSnapshot snapshot(String id) {
        return TourismContentSnapshot.builder().contentId(id).contentTypeId("12").title("현충사")
                .updatedAt(java.time.LocalDateTime.now()).build();
    }

    private TourismContentSnapshot snapshot(String id, String type, String mapx, String mapy) {
        return TourismContentSnapshot.builder().contentId(id).contentTypeId(type).title(id)
                .mapx(mapx == null ? null : new BigDecimal(mapx))
                .mapy(mapy == null ? null : new BigDecimal(mapy))
                .updatedAt(java.time.LocalDateTime.now()).build();
    }

    private TourismContentDetailResponse detail(String id, String type) {
        return TourismContentDetailResponse.builder()
                .common(TourismCommonDetailDto.builder().contentId(id).contentTypeId(type).title(id)
                        .mapx(new BigDecimal("culture".equals(id) ? "127.1000" : "127.1100"))
                        .mapy(new BigDecimal("culture".equals(id) ? "36.8000" : "36.8100")).build())
                .details(List.of()).build();
    }

    private AiPlanResponse.Content aiContent(int sequence, String id) {
        AiPlanResponse.Content content = new AiPlanResponse.Content();
        content.setSequence(sequence);
        content.setContentId(id);
        content.setTitle(id);
        return content;
    }

    private PlanContentsUpdateRequest.Day day(int number, String date, String id) {
        return day(number, date, new String[]{id});
    }

    private PlanContentsUpdateRequest.Day day(int number, String date, String... ids) {
        PlanContentsUpdateRequest.Day d = new PlanContentsUpdateRequest.Day();
        d.setDayNumber(number);
        d.setContentIds(List.of(ids));
        return d;
    }
}
