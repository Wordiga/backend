package com.wordiga.plan.controller;

import com.wordiga.plan.api.PlanApi;
import com.wordiga.plan.dto.*;
import com.wordiga.proposal.dto.ProposalResponse;
import com.wordiga.global.security.CurrentMemberId;
import com.wordiga.plan.service.PlanGenerationService;
import com.wordiga.plan.service.PlanService;
import com.wordiga.proposal.service.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController implements PlanApi {
    private final PlanService planService;
    private final PlanGenerationService planGenerationService;
    private final ProposalService proposalService;

    @PostMapping("/generate")
    public ResponseEntity<PlanDetailResponse> generatePlan(@CurrentMemberId Long memberId,
                                                           @Valid @RequestBody PlanGenerateRequest request) {
        return ResponseEntity.ok(planGenerationService.generate(memberId, request));
    }

    @GetMapping
    public ResponseEntity<PlanListResponse> getPlans(@CurrentMemberId Long memberId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size,
                                                     @RequestParam(defaultValue = "LATEST") PlanSort sort) {
        return ResponseEntity.ok(planService.getPlans(memberId, page, size, sort));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<PlanDetailResponse> getPlan(@CurrentMemberId Long memberId, @PathVariable Long planId) {
        return ResponseEntity.ok(planService.getPlan(memberId, planId));
    }

    @PatchMapping("/{planId}")
    public ResponseEntity<PlanDetailResponse> updatePlan(@CurrentMemberId Long memberId, @PathVariable Long planId,
                                                         @Valid @RequestBody PlanUpdateRequest request) {
        return ResponseEntity.ok(planService.updatePlan(memberId, planId, request));
    }

    @PutMapping("/{planId}/contents")
    public ResponseEntity<PlanDetailResponse> updateContents(@CurrentMemberId Long memberId, @PathVariable Long planId,
                                                             @Valid @RequestBody PlanContentsUpdateRequest request) {
        return ResponseEntity.ok(planService.updateContents(memberId, planId, request));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(@CurrentMemberId Long memberId, @PathVariable Long planId) {
        planService.deletePlan(memberId, planId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{planId}/proposals")
    public ResponseEntity<ProposalResponse> createProposal(@CurrentMemberId Long memberId, @PathVariable Long planId) {
        return ResponseEntity.ok(proposalService.create(memberId, planId));
    }

    @DeleteMapping("/{planId}/proposals/{proposalId}")
    public ResponseEntity<Void> deleteProposal(@CurrentMemberId Long memberId, @PathVariable Long planId,
                                               @PathVariable Long proposalId) {
        proposalService.delete(memberId, planId, proposalId);
        return ResponseEntity.noContent().build();
    }
}
