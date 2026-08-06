package com.wordiga.dto.plan;

import com.wordiga.plan.Plan;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PlanSummaryResponse {
    private Long planId;
    private String scheduleId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer participantCount;
    private String thumbnailUrl;
    private int contentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PlanSummaryResponse from(Plan p) {
        return builder().planId(p.getId()).scheduleId(p.getScheduleId()).title(p.getTitle())
                .startDate(p.getStartDate()).endDate(p.getEndDate()).participantCount(p.getParticipantCount())
                .thumbnailUrl(p.getPlanContents().isEmpty() ? null : p.getPlanContents().getFirst().getThumbnailUrl())
                .contentCount(p.getPlanContents().size()).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
}
