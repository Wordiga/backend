package com.wordiga.dto.ai;

import com.wordiga.dto.plan.PlanDetailResponse;
import com.wordiga.dto.proposal.ProposalCreateRequest;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AiProposalRequest {
    private String requestId; private String proposalTitle; private String organizationName;
    private String purpose; private String additionalRequest; private PlanDetailResponse plan;
    public static AiProposalRequest from(String requestId, ProposalCreateRequest r, PlanDetailResponse plan) {
        return builder().requestId(requestId).proposalTitle(r.getProposalTitle()).organizationName(r.getOrganizationName())
                .purpose(r.getPurpose()).additionalRequest(r.getAdditionalRequest()).plan(plan).build();
    }
}
