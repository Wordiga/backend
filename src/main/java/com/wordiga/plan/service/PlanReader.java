package com.wordiga.plan.service;

import com.wordiga.plan.dto.PlanDetailResponse;
import com.wordiga.plan.Plan;
import com.wordiga.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PlanReader {
    private final PlanRepository planRepository;

    @Transactional(readOnly = true, timeout = 5)
    public Snapshot read(Long memberId, Long planId) {
        Plan plan = planRepository.findByIdAndMemberId(planId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."));
        return new Snapshot(plan, PlanDetailResponse.from(plan));
    }

    public record Snapshot(Plan plan, PlanDetailResponse response) {
    }
}
