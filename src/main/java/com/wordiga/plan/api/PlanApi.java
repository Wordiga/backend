package com.wordiga.plan.api;

import com.wordiga.plan.dto.*;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.wordiga.proposal.dto.ProposalResponse;

public interface PlanApi {
    ResponseEntity<PlanDetailResponse> generatePlan(@Parameter(hidden = true) Long memberId, @Valid PlanGenerateRequest request);
    ResponseEntity<PlanListResponse> getPlans(@Parameter(hidden = true) Long memberId, @Min(0) int page, @Min(1) @Max(50) int size, PlanSort sort);
    ResponseEntity<PlanDetailResponse> getPlan(@Parameter(hidden = true) Long memberId, Long planId);
    @Operation(summary = "일정의 우천 대체 장소 조회", description = "실외 또는 판정 불가 장소별 근처 실내 후보를 반환합니다.")
    ResponseEntity<RainAlternativeResponse> getRainAlternatives(@Parameter(hidden = true) Long memberId, Long planId);
    ResponseEntity<PlanDetailResponse> updatePlan(@Parameter(hidden = true) Long memberId, Long planId, @Valid PlanUpdateRequest request);
    ResponseEntity<PlanDetailResponse> updateContents(@Parameter(hidden = true) Long memberId, Long planId, @Valid PlanContentsUpdateRequest request);
    ResponseEntity<Void> deletePlan(@Parameter(hidden = true) Long memberId, Long planId);
    ResponseEntity<ProposalResponse> createProposal(@Parameter(hidden = true) Long memberId, Long planId);
    ResponseEntity<Void> deleteProposal(@Parameter(hidden = true) Long memberId, Long planId, Long proposalId);
}
