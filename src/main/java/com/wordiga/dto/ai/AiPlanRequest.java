package com.wordiga.dto.ai;

import com.wordiga.dto.plan.PlanGenerateRequest;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class AiPlanRequest {
    private String requestId; private String title; private LocalDate startDate; private LocalDate endDate;
    private Integer participantCount; private List<String> ageGroups; private Integer maleRatio; private Integer femaleRatio;
    private Long budgetPerPerson; private String additionalRequest; private List<TourismContentDetailResponse> contents;
    public static AiPlanRequest from(String requestId, PlanGenerateRequest r, List<TourismContentDetailResponse> contents) {
        return builder().requestId(requestId).title(r.getTitle()).startDate(r.getStartDate()).endDate(r.getEndDate())
                .participantCount(r.getParticipantCount()).ageGroups(r.getAgeGroups()).maleRatio(r.getMaleRatio())
                .femaleRatio(r.getFemaleRatio()).budgetPerPerson(r.getBudgetPerPerson())
                .additionalRequest(r.getAdditionalRequest()).contents(contents).build();
    }
}
