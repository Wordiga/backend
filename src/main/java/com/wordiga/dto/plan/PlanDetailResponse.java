package com.wordiga.dto.plan;

import com.wordiga.domain.Plan;
import com.wordiga.domain.PlanContent;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Data @Builder
public class PlanDetailResponse {
    private Long planId; private String scheduleId; private String title; private LocalDate startDate; private LocalDate endDate;
    private Integer participantCount; private Object estimatedBudget; private List<Day> days; private List<String> warnings;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public static PlanDetailResponse from(Plan p, List<String> warnings) {
        List<Day> days = p.getPlanContents().stream().collect(Collectors.groupingBy(PlanContent::getDayNumber,
                TreeMap::new, Collectors.toList())).entrySet().stream().map(e -> Day.builder().dayNumber(e.getKey())
                .date(e.getValue().getFirst().getDate()).contents(e.getValue().stream()
                        .sorted(Comparator.comparing(PlanContent::getSequence)).map(Content::from).toList()).build()).toList();
        return builder().planId(p.getId()).title(p.getTitle()).startDate(p.getStartDate()).endDate(p.getEndDate())
                .participantCount(p.getParticipantCount()).days(days).warnings(warnings)
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
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
