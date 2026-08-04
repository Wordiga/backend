package com.wordiga.dto.ai;

import com.wordiga.dto.plan.PlanGenerateRequest;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class AiPlanRequest {
    private String requestId; private String title; private LocalDate startDate; private LocalDate endDate;
    private Integer participantCount; private List<String> ageGroups;
    private Long totalEstimatedCost; private List<AiPlanContent> contents;
    public static AiPlanRequest from(String requestId, PlanGenerateRequest r,
                                     List<com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse> details) {
        List<AiPlanContent> contents = details.stream().map(AiPlanContent::from).toList();
        List<Long> costs = contents.stream().map(AiPlanContent::getEstimatedCost).filter(java.util.Objects::nonNull).toList();
        return builder().requestId(requestId).title(r.getTitle()).startDate(r.getStartDate()).endDate(r.getEndDate())
                .participantCount(r.getParticipantCount()).ageGroups(r.getAgeGroups())
                .totalEstimatedCost(costs.isEmpty() ? null : costs.stream().mapToLong(Long::longValue).sum())
                .contents(contents).build();
    }
}
