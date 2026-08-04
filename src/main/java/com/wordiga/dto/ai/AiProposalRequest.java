package com.wordiga.dto.ai;

import com.wordiga.dto.plan.PlanDetailResponse;
import com.wordiga.dto.proposal.ProposalCreateRequest;
import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data @Builder
public class AiProposalRequest {
    private String requestId; private String proposalTitle; private String organizationName;
    private String purpose; private String additionalRequest; private PlanDetailResponse plan;
    @JsonProperty("visit_month") private Integer visitMonth;
    @JsonProperty("num_people") private Integer numPeople;
    public static AiProposalRequest from(String requestId, ProposalCreateRequest r, PlanDetailResponse plan) {
        return builder().requestId(requestId).proposalTitle(r.getProposalTitle()).organizationName(r.getOrganizationName())
                .purpose(r.getPurpose()).additionalRequest(r.getAdditionalRequest()).plan(plan)
                .visitMonth(plan.getStartDate() == null ? null : plan.getStartDate().getMonthValue())
                .numPeople(plan.getParticipantCount()).build();
    }
}
