package com.wordiga.plan.dto;

import com.wordiga.plan.Plan;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlanSummaryResponse {
    private Long planId;
    private Long scheduleId;
    private String title;
    private String visitMonth;
    private Integer stayDays;
    private Integer participantCount;
    private String thumbnailUrl;
    private int contentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PlanSummaryResponse from(Plan p) {
        return builder().planId(p.getId()).scheduleId(p.getScheduleId()).title(p.getTitle())
                .visitMonth(p.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")))
                .stayDays((int) java.time.temporal.ChronoUnit.DAYS.between(p.getStartDate(), p.getEndDate()) + 1)
                .participantCount(p.getParticipantCount())
                .thumbnailUrl(p.getPlanContents().isEmpty() ? null
                        : p.getPlanContents().getFirst().getContent().getFirstimage())
                .contentCount(p.getPlanContents().size()).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
}
