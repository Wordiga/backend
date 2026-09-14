package com.wordiga.global.client;

import com.wordiga.plan.dto.PlanDetailResponse;
import com.wordiga.plan.dto.ai.AiPlanResponse;
import com.wordiga.proposal.dto.AiProposalRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiServerContractUnitTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsCurrentScheduleResponseContract() throws Exception {
        AiPlanResponse response = objectMapper.readValue("""
                {"plan_id":1001,"status":"success","timetable":{"total_days":1,
                "total_travel_time_min":0,"days":[{"day":1,"date_label":"1일차","items":[{
                "order":1,"start_time":"10:00","end_time":"11:30","content_id":"126508",
                "title":"현충사","category":"tourist_spot","latitude":36.8,"longitude":126.9,
                "travel_time_from_prev_min":0,"travel_distance_meters":1200,"estimated_cost":5000,"memo":"관람"}]}]},
                "estimated_budget":{"total":50000,"per_person_amount":5000,"currency":"KRW","breakdown":{"activity":50000}},
                "warnings":[]}
                """, AiPlanResponse.class);

        assertThat(response.getScheduleId()).isEqualTo(1001L);
        assertThat(response.getDays()).hasSize(1);
        assertThat(response.getDays().getFirst().getContents().getFirst().getContentId()).isEqualTo("126508");
        assertThat(response.getDays().getFirst().getContents().getFirst().getDurationMinutes()).isEqualTo(90);
        assertThat(response.getDays().getFirst().getContents().getFirst().getTravelDistanceMeters()).isEqualTo(1200);
        assertThat(response.getDays().getFirst().getContents().getFirst().getEstimatedCost()).isEqualTo(5000L);
        assertThat(response.getEstimatedCost().getTotalAmount()).isEqualTo(50000L);
    }

    @Test
    void writesCurrentProposalRequestContract() throws Exception {
        String dynamicVisitMonth = java.time.YearMonth.now().plusMonths(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

        PlanDetailResponse.Content content = PlanDetailResponse.Content.builder().sequence(1).contentId("126508")
                .title("현충사").contentTypeId("12").addr1("충청남도 아산시")
                .thumbnailUrl("https://image.example/126508.jpg")
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 30)).travelTimeMinutes(0).build();
        PlanDetailResponse.Day day = PlanDetailResponse.Day.builder().dayNumber(1)
                .contents(List.of(content)).build();
        PlanDetailResponse plan = PlanDetailResponse.builder().planId(9L).visitMonth(dynamicVisitMonth)
                .participantCount(10).days(List.of(day)).build();
        String json = objectMapper.writeValueAsString(AiProposalRequest.from(plan));

        assertThat(json).contains("\"plan_id\":9", "\"company_info\"", "\"company_name\":\"Wordiga\"",
                "\"total_days\":1", "\"content_id\":\"126508\"");
    }
}
