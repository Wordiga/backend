package com.wordiga.plan.api;

import com.wordiga.plan.dto.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.wordiga.proposal.dto.ProposalCreateRequest;
import com.wordiga.proposal.dto.ProposalResponse;
import java.util.List;

public interface PlanApi {
    ResponseEntity<PlanDetailResponse> generatePlan(Long memberId, @Valid PlanGenerateRequest request);
    ResponseEntity<PlanListResponse> getPlans(Long memberId, @Min(0) int page, @Min(1) @Max(50) int size, PlanSort sort);
    ResponseEntity<PlanDetailResponse> getPlan(Long memberId, Long planId);
    ResponseEntity<PlanDetailResponse> updatePlan(Long memberId, Long planId, @Valid PlanUpdateRequest request);
    ResponseEntity<PlanDetailResponse> updateContents(Long memberId, Long planId, @Valid PlanContentsUpdateRequest request);
    ResponseEntity<Void> deletePlan(Long memberId, Long planId);
    ResponseEntity<ProposalResponse> createProposal(Long memberId, Long planId, @Valid ProposalCreateRequest request);
    ResponseEntity<List<ProposalResponse>> getProposals(Long memberId, Long planId);
    ResponseEntity<Void> deleteProposal(Long memberId, Long planId, Long proposalId);
}
