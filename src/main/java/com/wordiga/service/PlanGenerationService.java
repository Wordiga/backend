package com.wordiga.service;

import com.wordiga.client.AiServerClient;
import com.wordiga.dto.ai.*;
import com.wordiga.dto.plan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class PlanGenerationService {
    private final TourismContentDetailService tourismContentDetailService;
    private final AiServerClient aiServerClient;
    private final PlanWriter planWriter;

    public PlanDetailResponse generate(Long memberId, PlanGenerateRequest request) {
        validateRequest(request);
        var details = request.getSelectedContentIds().stream()
                .map(id -> tourismContentDetailService.getDetail(id, request.getStartDate(), request.getAgeGroups())).toList();
        AiPlanResponse response = aiServerClient.generatePlan(AiPlanRequest.from(UUID.randomUUID().toString(), request, details));
        validateResponse(request, response);
        return planWriter.saveGenerated(memberId, request, response);
    }
    private void validateRequest(PlanGenerateRequest r) {
        if (r.getStartDate().isAfter(r.getEndDate()) || ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate()) > 2)
            invalid(HttpStatus.BAD_REQUEST, "일정 기간은 1~3일이어야 합니다.");
        if (new HashSet<>(r.getSelectedContentIds()).size() != r.getSelectedContentIds().size())
            invalid(HttpStatus.BAD_REQUEST, "콘텐츠 ID는 중복될 수 없습니다.");
    }
    private void validateResponse(PlanGenerateRequest r, AiPlanResponse response) {
        if (response.getDays() == null || response.getDays().isEmpty()) invalid(HttpStatus.BAD_GATEWAY, "AI 일정이 비어 있습니다.");
        Set<String> selected = new HashSet<>(r.getSelectedContentIds());
        Set<String> scheduled = new HashSet<>();
        int expectedDay = 1;
        for (AiPlanResponse.Day day : response.getDays()) {
            if (day.getDayNumber() == null || day.getDayNumber() != expectedDay++ || day.getDate() == null
                    || !day.getDate().equals(r.getStartDate().plusDays(day.getDayNumber() - 1L)) || day.getContents() == null)
                invalid(HttpStatus.BAD_GATEWAY, "AI 일정의 날짜가 올바르지 않습니다.");
            int sequence = 1;
            for (AiPlanResponse.Content c : day.getContents())
                if (c.getSequence() == null || c.getSequence() != sequence++ || c.getContentId() == null
                        || !selected.contains(c.getContentId()) || !scheduled.add(c.getContentId()))
                    invalid(HttpStatus.BAD_GATEWAY, "AI 일정의 콘텐츠 순서가 올바르지 않습니다.");
        }
        if (!scheduled.equals(selected)) invalid(HttpStatus.BAD_GATEWAY, "AI 일정에 선택 콘텐츠가 모두 포함되어야 합니다.");
    }
    private void invalid(HttpStatus status, String message) { throw new ResponseStatusException(status, message); }
}
