package com.wordiga.plan.service;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.plan.Plan;
import com.wordiga.plan.PlanContent;
import com.wordiga.plan.CostSource;
import com.wordiga.plan.dto.*;
import com.wordiga.plan.repository.PlanRepository;
import com.wordiga.proposal.repository.ProposalRepository;
import com.wordiga.proposal.service.ProposalStorage;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import com.wordiga.tourism.domain.TourismContentSnapshotRepository;
import com.wordiga.tourism.service.TourismContentDetailService;
import com.wordiga.tourism.service.TourismContentSnapshotService;
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
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<String, ArrayDeque<PlanContent>> existingByContentId = plan.getPlanContents().stream()
                .collect(Collectors.groupingBy(content -> content.getContent().getContentId(),
                        LinkedHashMap::new, Collectors.toCollection(ArrayDeque::new)));
        List<PlanContent> contents = new ArrayList<>();
        int contentCount = 0;
        for (PlanContentsUpdateRequest.Day day : request.getDays()) {
            int sequence = 1;
            for (String contentId : day.getContentIds()) {
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
                PlanContent existing = existingByContentId.getOrDefault(c.getContentid(), new ArrayDeque<>()).pollFirst();
                if (existing != null) planContent.copyDetailsFrom(existing);
                if (planContent.getUnitAmount() == null) {
                    if (planContent.getEstimatedCost() == null) applyDefaultCost(planContent, plan.getParticipantCount());
                    else preserveLegacyCost(planContent, plan.getParticipantCount());
                }
                contents.add(planContent);
                contentCount++;
            }
        }
        if (contentCount > 10) invalid("콘텐츠는 최대 10개까지 저장할 수 있습니다.");
        plan.replaceContents(contents);
        recalculateBudget(plan);
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

    private void applyDefaultCost(PlanContent content, int participants) {
        var cost = PlanCostPolicy.defaultEstimate(content.getContent().getContentTypeId(), participants);
        content.updateCost(cost.amount(), cost.unit(), cost.quantity(), cost.calculatedAmount(),
                cost.calculatedAmount() / Math.max(1, participants), CostSource.DEFAULT);
    }

    private void preserveLegacyCost(PlanContent content, int participants) {
        var policy = PlanCostPolicy.defaultEstimate(content.getContent().getContentTypeId(), participants);
        int quantity = policy.unit() == com.wordiga.plan.CostUnit.PERSON ? participants : 1;
        content.updateCost(content.getEstimatedCost() / Math.max(1, quantity), policy.unit(), quantity,
                content.getEstimatedCost(), content.getEstimatedCost() / Math.max(1, participants), null);
    }

    private void recalculateBudget(Plan plan) {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        long total = 0;
        long perPerson = 0;
        for (PlanContent content : plan.getPlanContents()) {
            if (content.getEstimatedCost() != null) total += content.getEstimatedCost();
            if (content.getPerPersonShare() != null) {
                perPerson += content.getPerPersonShare();
                breakdown.merge(PlanCostPolicy.category(content.getContent().getContentTypeId()),
                        content.getPerPersonShare(), Long::sum);
            }
        }
        plan.applyAiResult(plan.getScheduleId(), total, perPerson, breakdown);
    }

    private void invalid(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
