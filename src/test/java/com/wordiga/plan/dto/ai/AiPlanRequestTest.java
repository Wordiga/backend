package com.wordiga.plan.dto.ai;

import tools.jackson.databind.json.JsonMapper;
import com.wordiga.plan.dto.PlanGenerateRequest;
import com.wordiga.tourism.dto.detail.TourismCommonDetailDto;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.tourism.dto.detail.TourismIntroDetailDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiPlanRequestTest {
    @Test
    void serializesConfirmedAiRequestSchema() throws Exception {
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setParticipantCount(25);
        request.setVisitMonth("2026-09");
        request.setStayDays(2);
        request.setAgeGroups(List.of("30대", "40대"));
        request.setSelectedContentIds(List.of("CT001"));
        TourismContentDetailResponse detail = TourismContentDetailResponse.builder()
                .common(TourismCommonDetailDto.builder().contentId("CT001").contentTypeId("32")
                        .title("숙소").addr1("충남 태안군").mapx(BigDecimal.valueOf(126.3))
                        .mapy(BigDecimal.valueOf(36.4)).lDongSignguCd("380")
                        .firstImage("https://image.example/CT001.jpg").build())
                .intro(TourismIntroDetailDto.builder().checkInTime("15:00").build())
                .build();

        var objectMapper = JsonMapper.builder().build();
        var aiRequest = AiPlanRequest.from(
                request, List.of(detail), List.of(detail), Map.of("CT001", List.of("숙박", "호텔")));
        var json = objectMapper.readTree(objectMapper.writeValueAsString(aiRequest));

        assertThat(json.get("visit_month").asInt()).isEqualTo(9);
        assertThat(json.get("num_people").asInt()).isEqualTo(25);
        assertThat(json.get("num_days").asInt()).isEqualTo(2);
        assertThat(json.has("num_nights")).isFalse();
        assertThat(json.get("saved_content_ids").get(0).asText()).isEqualTo("CT001");
        assertThat(json.get("saved_contents").get(0).get("category").asText()).isEqualTo("accommodation");
        assertThat(json.get("saved_contents").get(0).get("latitude").decimalValue()).isEqualByComparingTo("36.4");
        assertThat(json.get("saved_contents").get(0).get("tags").get(0).asText()).isEqualTo("숙박");
        assertThat(json.get("saved_contents").get(0).get("thumbnail").asText())
                .isEqualTo("https://image.example/CT001.jpg");
        assertThat(json.get("saved_contents").get(0).get("firstimage").asText())
                .isEqualTo("https://image.example/CT001.jpg");
        assertThat(json.get("regional_contents").isArray()).isTrue();
        assertThat(json.get("regional_contents").get(0).get("content_id").asText()).isEqualTo("CT001");
        assertThat(json.get("saved_contents").get(0).get("is_outdoor").asBoolean()).isFalse();
        assertThat(json.get("saved_contents").get(0).has("avg_visit_duration_min")).isFalse();
        assertThat(json.get("saved_contents").get(0).get("estimated_cost").get("quantity").asInt()).isEqualTo(1);
        assertThat(json.get("estimated_budget").get("currency").asText()).isEqualTo("KRW");
        assertThat(json.get("estimated_budget").get("breakdown").get("숙박").asLong()).isEqualTo(4_000);
        assertThat(json.has("monthly_weather")).isFalse();
    }
}
