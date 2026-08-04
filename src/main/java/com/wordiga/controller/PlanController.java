package com.wordiga.controller;

import com.wordiga.api.PlanApi;
import com.wordiga.dto.plan.*;
import com.wordiga.security.CurrentMemberId;
import com.wordiga.service.PlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
                                                         @RequestBody PlanUpdateRequest request) {
        return ResponseEntity.ok(planService.updatePlan(memberId, planId, request));
    }
    @PutMapping("/{planId}/contents")
    public ResponseEntity<PlanDetailResponse> updateContents(@CurrentMemberId Long memberId, @PathVariable Long planId,
                                                              @RequestBody PlanContentsUpdateRequest request) {
        return ResponseEntity.ok(planService.updateContents(memberId, planId, request));
    }
    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(@CurrentMemberId Long memberId, @PathVariable Long planId) {
        planService.deletePlan(memberId, planId);
        return ResponseEntity.noContent().build();
    }
}
