package com.wordiga.service;

import com.wordiga.client.AiServerClient;
import com.wordiga.client.TourismApiClient;
import com.wordiga.dto.ai.*;
import com.wordiga.dto.plan.*;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service @RequiredArgsConstructor
public class PlanGenerationService {
    private final TourismContentDetailService tourismContentDetailService;
    private final RegionalContentService regionalContentService;
    private final TourismApiClient tourismApiClient;
    private final AiServerClient aiServerClient;
    private final PlanWriter planWriter;

    public PlanDetailResponse generate(Long memberId, PlanGenerateRequest request) {
        validateRequest(request);
        List<TourismContentDetailResponse> details = new ArrayList<>();
        for (int index = 0; index < request.getSelectedContentIds().size(); index++)
            details.add(tourismContentDetailService.getAiDetail(
                    request.getSelectedContentIds().get(index), request.getStartDate(), index == 0));
        var regional = regionalContentService.find(request, details);
        List<TourismContentDetailResponse> all = new ArrayList<>(details); all.addAll(regional);
        AiPlanResponse response = aiServerClient.generatePlan(
                AiPlanRequest.from(request, details, regional, resolveTags(all)));
        validateResponse(request, regional, response);
        String sigunguCode = details.getFirst().getCommon().getLDongSignguCd();
        String sigunguName = sigunguCode == null ? null : ChungnamSigungu.NAMES.get(sigunguCode);
        return planWriter.saveGenerated(memberId, request, response, sigunguName);
    }

    private Map<String, List<String>> resolveTags(List<TourismContentDetailResponse> details) {
        Map<List<String>, List<String>> cache = new HashMap<>();
        Map<String, List<String>> result = new HashMap<>();
        for (var detail : details) {
            var common = detail.getCommon();
            List<String> codes = List.of(value(common.getLclsSystm1()), value(common.getLclsSystm2()),
                    value(common.getLclsSystm3()));
            if (codes.get(0).isEmpty()) continue;
            List<String> names = cache.get(codes);
            if (cache.containsKey(codes)) {
                if (names != null) result.put(common.getContentId(), names);
                continue;
            }
            try {
                names = tourismApiClient.fetchClassificationNames(
                        emptyToNull(codes.get(0)), emptyToNull(codes.get(1)), emptyToNull(codes.get(2)));
                cache.put(codes, names);
                if (names != null) result.put(common.getContentId(), names);
            } catch (RuntimeException exception) {
                cache.put(codes, null);
                log.warn("[AI 일정] 관광 분류명 조회 실패: contentId={}, codes={}",
                        common.getContentId(), codes, exception);
            }
        }
        return result;
    }

    private String value(String value) { return value == null ? "" : value; }
    private String emptyToNull(String value) { return value.isEmpty() ? null : value; }
    private void validateRequest(PlanGenerateRequest r) {
        if (r.getStartDate().isAfter(r.getEndDate()) || ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate()) > 2)
            invalid(HttpStatus.BAD_REQUEST, "일정 기간은 1~3일이어야 합니다.");
        if (new HashSet<>(r.getSelectedContentIds()).size() != r.getSelectedContentIds().size())
            invalid(HttpStatus.BAD_REQUEST, "콘텐츠 ID는 중복될 수 없습니다.");
        if (r.getVisitMonth() != null && r.getVisitMonth() != r.getStartDate().getMonthValue())
            invalid(HttpStatus.BAD_REQUEST, "방문 월은 시작일의 월과 같아야 합니다.");
        long maximumNights = ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate());
        if (r.getStayNights() != null && r.getStayNights() > maximumNights)
            invalid(HttpStatus.BAD_REQUEST, "숙박 수는 일정 기간보다 클 수 없습니다.");
    }
    private void validateResponse(PlanGenerateRequest r, List<TourismContentDetailResponse> regional,
                                  AiPlanResponse response) {
        if (response.getDays() == null || response.getDays().isEmpty()) invalid(HttpStatus.BAD_GATEWAY, "AI 일정이 비어 있습니다.");
        Set<String> selected = new HashSet<>(r.getSelectedContentIds());
        Set<String> allowed = new HashSet<>(selected);
        regional.forEach(detail -> allowed.add(detail.getCommon().getContentId()));
        Set<String> scheduled = new HashSet<>();
        int expectedDay = 1;
        for (AiPlanResponse.Day day : response.getDays()) {
            if (day.getDayNumber() == null || day.getDayNumber() != expectedDay++ || day.getDate() == null
                    || !day.getDate().equals(r.getStartDate().plusDays(day.getDayNumber() - 1L)) || day.getContents() == null)
                invalid(HttpStatus.BAD_GATEWAY, "AI 일정의 날짜가 올바르지 않습니다.");
            int sequence = 1;
            for (AiPlanResponse.Content c : day.getContents())
                if (c.getSequence() == null || c.getSequence() != sequence++ || c.getContentId() == null
                        || !allowed.contains(c.getContentId()) || !scheduled.add(c.getContentId()))
                    invalid(HttpStatus.BAD_GATEWAY, "AI 일정의 콘텐츠 순서가 올바르지 않습니다.");
        }
        if (!scheduled.containsAll(selected)) invalid(HttpStatus.BAD_GATEWAY, "AI 일정에 선택 콘텐츠가 모두 포함되어야 합니다.");
    }
    private void invalid(HttpStatus status, String message) { throw new ResponseStatusException(status, message); }
}
