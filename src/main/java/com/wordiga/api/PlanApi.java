package com.wordiga.api;

import com.wordiga.dto.plan.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public interface PlanApi {
    ResponseEntity<PlanListResponse> getPlans(Long memberId, @Min(0) int page, @Min(1) @Max(50) int size, PlanSort sort);
    ResponseEntity<PlanDetailResponse> getPlan(Long memberId, Long planId);
    ResponseEntity<PlanDetailResponse> updatePlan(Long memberId, Long planId, @Valid PlanUpdateRequest request);
    ResponseEntity<PlanDetailResponse> updateContents(Long memberId, Long planId, @Valid PlanContentsUpdateRequest request);
    ResponseEntity<Void> deletePlan(Long memberId, Long planId);
}
