package com.wordiga.plan.dto;

import com.wordiga.plan.Plan;
import com.wordiga.plan.PlanContent;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
public class PlanDetailResponse {
    private Long planId;
    private Long scheduleId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer participantCount;
    private EstimatedBudget estimatedBudget;
    private List<Day> days;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PlanDetailResponse from(Plan p) {
        List<Day> days = p.getPlanContents().stream().collect(Collectors.groupingBy(PlanContent::getDayNumber,
                TreeMap::new, Collectors.toList())).entrySet().stream().map(e -> Day.builder().dayNumber(e.getKey())
                .date(p.getStartDate().plusDays(e.getKey() - 1L)).contents(e.getValue().stream()
                        .sorted(Comparator.comparing(PlanContent::getSequence)).map(Content::from).toList()).build()).toList();
        EstimatedBudget budget = null;
        if (p.getEstimatedTotalAmount() != null || p.getEstimatedPerPersonAmount() != null) {
            java.util.Map<String, Long> breakdown = p.getBudgetBreakdowns().stream().collect(Collectors.toMap(
                    com.wordiga.plan.PlanBudgetBreakdown::getCategory, com.wordiga.plan.PlanBudgetBreakdown::getAmount,
                    (first, ignored) -> first, LinkedHashMap::new));
            budget = new EstimatedBudget(p.getEstimatedTotalAmount(), p.getEstimatedPerPersonAmount(),
                    "KRW", breakdown);
        }
        return builder().planId(p.getId()).scheduleId(p.getScheduleId()).title(p.getTitle()).startDate(p.getStartDate()).endDate(p.getEndDate())
                .participantCount(p.getParticipantCount()).estimatedBudget(budget).days(days)
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }

    public record EstimatedBudget(Long totalAmount, Long perPersonAmount, String currency,
                                  Map<String, Long> breakdown) {
    }

    @Data
    @Builder
    public static class Day {
        private Integer dayNumber;
        private LocalDate date;
        private List<Content> contents;
    }

    @Data
    @Builder
    public static class Content {
        private Integer sequence;
        private String contentId;
        private String title;
        private String contentTypeId;
        private String addr1;
        private BigDecimal mapx;
        private BigDecimal mapy;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer durationMinutes;
        private Integer travelTimeMinutes;
        private Integer travelDistanceMeters;
        private Long estimatedCost;
        private String memo;

        static Content from(PlanContent c) {
            var content = c.getContent();
            return builder().sequence(c.getSequence()).contentId(content.getContentId())
                    .title(content.getTitle()).contentTypeId(content.getContentTypeId()).addr1(content.getAddr1())
                    .mapx(content.getMapx()).mapy(content.getMapy())
                    .startTime(c.getStartTime()).endTime(c.getEndTime()).durationMinutes(c.getDuration())
                    .travelTimeMinutes(c.getTravelTimeMinutes()).travelDistanceMeters(c.getTravelDistanceMeters())
                    .estimatedCost(c.getEstimatedCost()).build();
        }
    }
}
