package com.wordiga.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordiga.domain.Plan;
import com.wordiga.domain.PlanContent;
import com.wordiga.dto.ai.AiPlanResponse;
import com.wordiga.dto.plan.PlanDetailResponse;
import com.wordiga.dto.plan.PlanGenerateRequest;
import com.wordiga.repository.MemberRepository;
import com.wordiga.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.format.DateTimeFormatter;

@Service @RequiredArgsConstructor
public class PlanWriter {
    private final PlanRepository planRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    @Transactional(timeout = 5)
    public PlanDetailResponse saveGenerated(Long memberId, PlanGenerateRequest request, AiPlanResponse ai,
                                            String sigunguName) {
        var member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원을 찾을 수 없습니다."));
        Plan plan = Plan.create(member, title(memberId, request, sigunguName),
                request.getStartDate(), request.getEndDate(), request.getParticipantCount(), "AI");
        for (AiPlanResponse.Day day : ai.getDays()) for (AiPlanResponse.Content c : day.getContents())
            plan.addContent(PlanContent.createFromAi(plan, day.getDayNumber(), day.getDate(), c.getSequence(),
                    c.getContentId(), c.getTitle(), c.getContentTypeId(), c.getAddr1(), c.getMapx(), c.getMapy(),
                    c.getStartTime(), c.getEndTime(), c.getDurationMinutes(), c.getTravelTimeMinutes(),
                    c.getTravelDistanceMeters(), c.getEstimatedCost(), c.getMemo()));
        AiPlanResponse.EstimatedBudget budget = ai.getEstimatedBudget();
        plan.applyAiResult(ai.getScheduleId(), json(ai), budget == null ? null : budget.getTotalAmount(),
                budget == null ? null : budget.getPerPersonAmount(), budget == null ? null : json(budget.getBreakdown()));
        return PlanDetailResponse.from(planRepository.save(plan));
    }
    private String title(Long memberId, PlanGenerateRequest request, String sigunguName) {
        if (request.getTitle() != null && !request.getTitle().isBlank()) return request.getTitle().strip();
        String base = (sigunguName == null ? "충남" : sigunguName) + " "
                + request.getStartDate().format(DateTimeFormatter.ofPattern("MMdd"));
        if (!planRepository.existsByMemberIdAndTitle(memberId, base)) return base;
        int number = 1;
        while (planRepository.existsByMemberIdAndTitle(memberId, base + " " + number)) number++;
        return base + " " + number;
    }
    private String json(AiPlanResponse response) {
        try { return objectMapper.writeValueAsString(response); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답을 저장할 수 없습니다.", e); }
    }
    private String json(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 예산 응답을 저장할 수 없습니다.", e); }
    }
}
