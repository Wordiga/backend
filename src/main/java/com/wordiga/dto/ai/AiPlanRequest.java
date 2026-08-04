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
    private Integer participantCount; private List<String> ageGroups;
    private Long budgetPerPerson; private List<TourismContentDetailResponse> contents;
    public static AiPlanRequest from(String requestId, PlanGenerateRequest r, List<TourismContentDetailResponse> contents) {
        return builder().requestId(requestId).title(r.getTitle()).startDate(r.getStartDate()).endDate(r.getEndDate())
                .participantCount(r.getParticipantCount()).ageGroups(r.getAgeGroups())
                .budgetPerPerson(r.getBudgetPerPerson()).contents(contents).build();
    }
}
