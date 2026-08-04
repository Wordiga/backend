package com.wordiga.dto.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordiga.dto.plan.PlanGenerateRequest;
import com.wordiga.dto.tourismContent.detail.TourismCommonDetailDto;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import com.wordiga.dto.tourismContent.detail.TourismIntroDetailDto;
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
        request.setStartDate(LocalDate.of(2026, 9, 1));
        request.setEndDate(LocalDate.of(2026, 9, 2));
        request.setParticipantCount(25);
        request.setAgeGroups(List.of("30대", "40대"));
        request.setSelectedContentIds(List.of("CT001"));
        TourismContentDetailResponse detail = TourismContentDetailResponse.builder()
                .common(TourismCommonDetailDto.builder().contentId("CT001").contentTypeId("32")
                        .title("숙소").addr1("충남 태안군").mapx(BigDecimal.valueOf(126.3))
                        .mapy(BigDecimal.valueOf(36.4)).lDongSignguCd("380").build())
                .intro(TourismIntroDetailDto.builder().checkInTime("15:00").build()).build();

        var objectMapper = new ObjectMapper();
        var aiRequest = AiPlanRequest.from(
                request, List.of(detail), Map.of("CT001", List.of("숙박", "호텔")));
        var json = objectMapper.readTree(objectMapper.writeValueAsString(aiRequest));

        assertThat(json.get("visit_month").asInt()).isEqualTo(9);
        assertThat(json.get("num_people").asInt()).isEqualTo(25);
        assertThat(json.get("num_days").asInt()).isEqualTo(2);
        assertThat(json.get("saved_content_ids").get(0).asText()).isEqualTo("CT001");
        assertThat(json.get("saved_contents").get(0).get("category").asText()).isEqualTo("accommodation");
        assertThat(json.get("saved_contents").get(0).get("latitude").decimalValue()).isEqualByComparingTo("36.4");
        assertThat(json.get("saved_contents").get(0).get("tags").get(0).asText()).isEqualTo("숙박");
        assertThat(json.get("regional_contents").isArray()).isTrue();
    }
}
