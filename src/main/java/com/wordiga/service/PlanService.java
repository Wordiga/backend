package com.wordiga.service;

import com.wordiga.domain.Plan;
import com.wordiga.domain.PlanContent;
import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.plan.*;
import com.wordiga.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class PlanService {
    private final PlanRepository planRepository;
    private final TourismContentDetailService tourismContentDetailService;

    public PlanListResponse getPlans(Long memberId, int page, int size, PlanSort sort) {
        Sort order = sort == PlanSort.START_DATE_ASC ? Sort.by("startDate", "id").ascending()
                : Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Page<Plan> result = planRepository.findByMemberId(memberId, PageRequest.of(page, size, order));
        return PlanListResponse.builder().items(result.map(PlanSummaryResponse::from).getContent())
                .page(page).size(size).hasNext(result.hasNext()).build();
    }

    public PlanDetailResponse getPlan(Long memberId, Long planId) { return detail(owned(memberId, planId), List.of()); }

    @Transactional
    public PlanDetailResponse updatePlan(Long memberId, Long planId, PlanUpdateRequest request) {
        if ((request.getTitle() == null && request.getParticipantCount() == null)
                || (request.getTitle() != null && request.getTitle().isBlank())) invalid("수정할 값을 확인해 주세요.");
        Plan plan = owned(memberId, planId);
        plan.update(request.getTitle(), request.getParticipantCount());
        return detail(plan, List.of());
    }

    @Transactional
    public PlanDetailResponse updateContents(Long memberId, Long planId, PlanContentsUpdateRequest request) {
        Plan plan = owned(memberId, planId);
        validateDays(plan, request.getDays());
        List<PlanContent> contents = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (PlanContentsUpdateRequest.Day day : request.getDays()) {
            int sequence = 1;
            for (String contentId : day.getContentIds()) {
                if (!ids.add(contentId)) invalid("콘텐츠 ID는 중복될 수 없습니다.");
                ContentDetailDto c = tourismContentDetailService.getCommonDetail(contentId);
                contents.add(PlanContent.createForUpdate(plan, day.getDayNumber(), day.getDate(), sequence++, contentId,
                        c.getTitle(), c.getContenttypeid(), c.getAddr1(), c.getFirstimage(), decimal(c.getMapx()), decimal(c.getMapy())));
            }
        }
        if (ids.size() > 10) invalid("콘텐츠는 최대 10개까지 저장할 수 있습니다.");
        plan.replaceContents(contents);
        return detail(plan, List.of("콘텐츠 배치가 변경되어 이동시간을 다시 계산해야 합니다."));
    }

    @Transactional public void deletePlan(Long memberId, Long planId) { planRepository.delete(owned(memberId, planId)); }

    private Plan owned(Long memberId, Long planId) { return planRepository.findByIdAndMemberId(planId, memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.")); }
    private PlanDetailResponse detail(Plan p, List<String> warnings) { return PlanDetailResponse.from(p, warnings); }
    private void validateDays(Plan p, List<PlanContentsUpdateRequest.Day> days) {
        LocalDate expected = p.getStartDate(); int number = 1;
        for (PlanContentsUpdateRequest.Day day : days) {
            if (day.getDayNumber() != number++ || !day.getDate().equals(expected) || day.getDate().isAfter(p.getEndDate()))
                invalid("일차와 날짜는 일정 기간 안에서 연속되어야 합니다.");
            expected = expected.plusDays(1);
        }
        if (!expected.minusDays(1).equals(p.getEndDate())) invalid("모든 일정 날짜를 전달해야 합니다.");
    }
    private BigDecimal decimal(String value) { try { return value == null || value.isBlank() ? null : new BigDecimal(value); }
        catch (NumberFormatException e) { return null; } }
    private void invalid(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
