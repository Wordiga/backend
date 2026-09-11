package com.wordiga.plan.api;

import com.wordiga.plan.dto.*;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.wordiga.proposal.dto.ProposalResponse;

public interface PlanApi {
    ResponseEntity<PlanDetailResponse> generatePlan(@Parameter(hidden = true) Long memberId, @Valid PlanGenerateRequest request);
    ResponseEntity<PlanListResponse> getPlans(@Parameter(hidden = true) Long memberId, @Min(0) int page, @Min(1) @Max(50) int size, PlanSort sort);
    ResponseEntity<PlanDetailResponse> getPlan(@Parameter(hidden = true) Long memberId, Long planId);
    ResponseEntity<PlanDetailResponse> updatePlan(@Parameter(hidden = true) Long memberId, Long planId, @Valid PlanUpdateRequest request);
    ResponseEntity<PlanDetailResponse> updateContents(@Parameter(hidden = true) Long memberId, Long planId, @Valid PlanContentsUpdateRequest request);
    ResponseEntity<Void> deletePlan(@Parameter(hidden = true) Long memberId, Long planId);
    ResponseEntity<ProposalResponse> createProposal(@Parameter(hidden = true) Long memberId, Long planId);
    ResponseEntity<Void> deleteProposal(@Parameter(hidden = true) Long memberId, Long planId, Long proposalId);
}
