package com.wordiga.plan.service;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.plan.Plan;
import com.wordiga.plan.PlanContent;
import com.wordiga.plan.dto.*;
import com.wordiga.plan.repository.PlanRepository;
import com.wordiga.proposal.repository.ProposalRepository;
import com.wordiga.proposal.service.ProposalStorage;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import com.wordiga.tourism.domain.TourismContentSnapshotRepository;
import com.wordiga.tourism.service.TourismContentDetailService;
import com.wordiga.tourism.service.TourismContentSnapshotService;
import com.wordiga.tourism.service.MonthlyWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.wordiga.global.util.KtoUtils.parseBigDecimal;
import static com.wordiga.global.util.KtoUtils.parseKtoDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {
    private final PlanRepository planRepository;
    private final TourismContentDetailService tourismContentDetailService;
    private final TourismContentSnapshotRepository snapshotRepository;
    private final TourismContentSnapshotService snapshotService;
    private final ProposalRepository proposalRepository;
    private final ProposalStorage proposalStorage;
    private final MonthlyWeatherService monthlyWeatherService;

    @Transactional
    public PlanListResponse getPlans(Long memberId, int page, int size, PlanSort sort) {
        Sort order = sort == PlanSort.START_DATE_ASC ? Sort.by("startDate", "id").ascending()
                : Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Page<Plan> result = planRepository.findByMemberId(memberId, PageRequest.of(page, size, order));
        synchronize(result.getContent());
        return PlanListResponse.builder().items(result.map(PlanSummaryResponse::from).getContent())
                .page(page).size(size).totalPages(result.getTotalPages()).hasNext(result.hasNext()).build();
    }

    @Transactional
    public PlanDetailResponse getPlan(Long memberId, Long planId) {
        Plan plan = owned(memberId, planId);
        synchronize(List.of(plan));
        return detail(plan);
    }

    public PlanWeatherResponse getWeather(Long memberId, Long planId, int month) {
        Plan plan = owned(memberId, planId);
        TourismContentSnapshot representative = plan.getPlanContents().stream()
                .sorted(java.util.Comparator.comparing(PlanContent::getDayNumber)
                        .thenComparing(PlanContent::getSequence))
                .map(PlanContent::getContent)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "날씨를 조회할 일정 콘텐츠가 없습니다."));
        var weather = monthlyWeatherService.estimate(representative.getSigunguCode(), month);
        if (weather == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "일정 지역의 날씨를 조회할 수 없습니다.");
        return PlanWeatherResponse.from(representative.getSigunguName(), weather);
    }

    @Transactional
    public PlanDetailResponse updatePlan(Long memberId, Long planId, PlanUpdateRequest request) {
        if ((request.getTitle() == null && request.getParticipantCount() == null)
                || (request.getTitle() != null && request.getTitle().isBlank())) invalid("수정할 값을 확인해 주세요.");
        Plan plan = owned(memberId, planId);
        plan.update(request.getTitle(), request.getParticipantCount());
        synchronize(List.of(plan));
        return detail(plan);
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

                TourismContentSnapshot snapshot = snapshotRepository.save(TourismContentSnapshot.builder()
                        .contentId(c.getContentid())
                        .contentTypeId(c.getContenttypeid())
                        .title(c.getTitle())
                        .firstimage(c.getFirstimage())
                        .addr1(c.getAddr1())
                        .mapx(parseBigDecimal(c.getMapx()))
                        .mapy(parseBigDecimal(c.getMapy()))
                        .sigunguCode(c.getLDongSignguCd())
                        .lclsSystem1Code(c.getLclsSystm1())
                        .lclsSystem2Code(c.getLclsSystm2())
                        .lclsSystem3Code(c.getLclsSystm3())
                        .sourceModifiedAt(parseKtoDateTime(c.getModifiedtime()))
                        .updatedAt(LocalDateTime.now())
                        .build());

                PlanContent planContent = PlanContent.create(plan, sequence++, day.getDayNumber(), snapshot);
                contents.add(planContent);
            }
        }
        if (ids.size() > 10) invalid("콘텐츠는 최대 10개까지 저장할 수 있습니다.");
        plan.replaceContents(contents);
        return detail(plan);
    }

    @Transactional
    public void deletePlan(Long memberId, Long planId) {
        planRepository.delete(owned(memberId, planId));
    }

    private Plan owned(Long memberId, Long planId) {
        return planRepository.findByIdAndMemberId(planId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."));
    }

    private PlanDetailResponse detail(Plan p) {
        PlanDetailResponse.ProposalInfo proposalInfo = proposalRepository
                .findByPlanIdAndExpiresAtAfter(p.getId(), LocalDateTime.now())
                .map(proposal -> {
                    String docxUrl = proposalStorage.url(proposal.getS3Key(), proposal.getFileName(), true);
                    String pdfUrl = proposal.getS3KeyPdf() != null
                            ? proposalStorage.url(proposal.getS3KeyPdf(), proposal.getFileName().replace(".docx", ".pdf"), false)
                            : null;
                    return new PlanDetailResponse.ProposalInfo(
                            proposal.getFileName(), pdfUrl, docxUrl, proposal.getCreatedAt());
                }).orElse(null);
        return PlanDetailResponse.from(p, proposalInfo);
    }

    private void synchronize(List<Plan> plans) {
        snapshotService.synchronizeReferenced(plans.stream()
                .flatMap(plan -> plan.getPlanContents().stream())
                .map(content -> content.getContent().getContentId())
                .toList());
    }

    private void validateDays(Plan p, List<PlanContentsUpdateRequest.Day> days) {
        int number = 1;
        for (PlanContentsUpdateRequest.Day day : days) {
            if (day.getDayNumber() != number++) invalid("일차는 1부터 연속되어야 합니다.");
        }
        if (days.size() != java.time.temporal.ChronoUnit.DAYS.between(p.getStartDate(), p.getEndDate()) + 1)
            invalid("모든 일정 일차를 전달해야 합니다.");
    }

    private void invalid(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
