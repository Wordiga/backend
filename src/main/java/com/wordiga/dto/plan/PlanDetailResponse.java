package com.wordiga.dto.plan;

import com.wordiga.domain.Plan;
import com.wordiga.domain.PlanContent;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@Data @Builder
public class PlanDetailResponse {
    private static final ObjectMapper JSON = new ObjectMapper();
    private Long planId; private String scheduleId; private String title; private LocalDate startDate; private LocalDate endDate;
    private Integer participantCount; private Object estimatedBudget; private List<Day> days;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public static PlanDetailResponse from(Plan p) {
        List<Day> days = p.getPlanContents().stream().collect(Collectors.groupingBy(PlanContent::getDayNumber,
                TreeMap::new, Collectors.toList())).entrySet().stream().map(e -> Day.builder().dayNumber(e.getKey())
                .date(e.getValue().getFirst().getDate()).contents(e.getValue().stream()
                        .sorted(Comparator.comparing(PlanContent::getSequence)).map(Content::from).toList()).build()).toList();
        java.util.Map<String, Object> budget = null;
        if (p.getEstimatedTotalAmount() != null || p.getEstimatedPerPersonAmount() != null) {
            budget = new java.util.LinkedHashMap<>();
            budget.put("totalAmount", p.getEstimatedTotalAmount()); budget.put("perPersonAmount", p.getEstimatedPerPersonAmount());
            budget.put("currency", "KRW");
            if (p.getEstimatedBudgetBreakdown() != null) budget.put("breakdown", breakdown(p.getEstimatedBudgetBreakdown()));
        }
        return builder().planId(p.getId()).scheduleId(p.getScheduleId()).title(p.getTitle()).startDate(p.getStartDate()).endDate(p.getEndDate())
                .participantCount(p.getParticipantCount()).estimatedBudget(budget).days(days)
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
    private static Object breakdown(String value) {
        try { return JSON.readTree(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ignored) { return value; }
    }
    @Data @Builder public static class Day { private Integer dayNumber; private LocalDate date; private List<Content> contents; }
    @Data @Builder public static class Content {
        private Integer sequence; private String contentId; private String title; private String contentTypeId;
        private String addr1; private BigDecimal mapx; private BigDecimal mapy; private LocalTime startTime; private LocalTime endTime;
        private Integer durationMinutes; private Integer travelTimeMinutes; private Integer travelDistanceMeters;
        private Long estimatedCost; private String memo;
        static Content from(PlanContent c) { return builder().sequence(c.getSequence()).contentId(c.getContentId())
                .title(c.getContentTitle()).contentTypeId(c.getContentTypeId()).addr1(c.getAddr1()).mapx(c.getMapx()).mapy(c.getMapy())
                .startTime(c.getStartTime()).endTime(c.getEndTime()).durationMinutes(c.getDuration())
                .travelTimeMinutes(c.getTravelTimeMinutes()).travelDistanceMeters(c.getTravelDistanceMeters())
                .estimatedCost(c.getEstimatedCost()).memo(c.getMemo()).build(); }
    }
}
