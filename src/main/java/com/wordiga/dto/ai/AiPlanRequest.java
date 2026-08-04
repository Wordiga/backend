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
    private CostRange totalCostRange; private List<AiPlanContent> contents;
    public static AiPlanRequest from(String requestId, PlanGenerateRequest r,
                                     List<com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse> details) {
        List<AiPlanContent> contents = details.stream().map(detail -> AiPlanContent.from(detail, r.getParticipantCount())).toList();
        long minimum = contents.stream().map(AiPlanContent::getCost).map(AiPlanContent.Cost::getCalculatedAmount)
                .filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
        long maximum = contents.stream().map(AiPlanContent::getCost)
                .map(cost -> cost.getCalculatedAmount() != null ? cost.getCalculatedAmount() : cost.getFallbackCalculatedAmount())
                .filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
        return builder().requestId(requestId).title(r.getTitle()).startDate(r.getStartDate()).endDate(r.getEndDate())
                .participantCount(r.getParticipantCount()).ageGroups(r.getAgeGroups())
                .totalCostRange(new CostRange(minimum, maximum))
                .contents(contents).build();
    }
    public record CostRange(long minimumAmount, long maximumAmount) { }
}
