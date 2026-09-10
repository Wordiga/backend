package com.wordiga.plan.service;

import com.wordiga.global.config.TourismProperties;
import com.wordiga.member.Member;
import com.wordiga.member.repository.MemberRepository;
import com.wordiga.plan.Plan;
import com.wordiga.plan.PlanContent;
import com.wordiga.plan.dto.PlanDetailResponse;
import com.wordiga.plan.dto.PlanGenerateRequest;
import com.wordiga.plan.dto.ai.AiPlanResponse;
import com.wordiga.plan.repository.PlanRepository;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import com.wordiga.tourism.domain.TourismContentSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.wordiga.global.util.KtoUtils.parseKtoDateTime;

@Service
@RequiredArgsConstructor
public class PlanWriter {
    private final PlanRepository planRepository;
    private final MemberRepository memberRepository;
    private final TourismContentSnapshotRepository snapshotRepository;
    private final TourismProperties tourismProperties;

    @Transactional(timeout = 5)
    public PlanDetailResponse saveGenerated(Long memberId, PlanGenerateRequest request, AiPlanResponse ai,
                                            String sigunguName) {
        return saveGenerated(memberId, request, ai, List.of(), sigunguName);
    }

    @Transactional(timeout = 5)
    public PlanDetailResponse saveGenerated(Long memberId, PlanGenerateRequest request, AiPlanResponse ai,
                                            List<com.wordiga.tourism.dto.detail.TourismContentDetailResponse> details,
                                            String sigunguName) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원을 찾을 수 없습니다."));

        details.forEach(detail -> {
            var common = detail.getCommon();
            snapshotRepository.save(TourismContentSnapshot.builder()
                    .contentId(common.getContentId()).contentTypeId(common.getContentTypeId())
                    .title(common.getTitle()).firstimage(common.getFirstImage()).addr1(common.getAddr1())
                    .mapx(common.getMapx()).mapy(common.getMapy()).sigunguCode(common.getLDongSignguCd())
                    .sigunguName(sigunguName).lclsSystem1Code(common.getLclsSystm1())
                    .lclsSystem2Code(common.getLclsSystm2()).lclsSystem3Code(common.getLclsSystm3())
                    .sourceModifiedAt(parseKtoDateTime(common.getModifiedTime()))
                    .updatedAt(LocalDateTime.now()).build());
        });

        Plan plan = Plan.create(member, title(memberId, request, sigunguName),
                request.getStartDate(), request.getEndDate(), request.getParticipantCount());

        for (AiPlanResponse.Day day : ai.getDays()) {
            for (AiPlanResponse.Content c : day.getContents()) {
                TourismContentSnapshot snapshot = snapshotRepository.findById(c.getContentId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "콘텐츠 스냅샷을 찾을 수 없습니다."));

                PlanContent planContent = PlanContent.create(plan, c.getSequence(), day.getDayNumber(), snapshot);
                planContent.updateAiDetails(
                        null,
                        c.getDurationMinutes(),
                        c.getStartTime(),
                        c.getEndTime(),
                        c.getTravelTimeMinutes(),
                        c.getTravelDistanceMeters(),
                        c.getEstimatedCost()
                );
                plan.addContent(planContent);
            }
        }

        AiPlanResponse.EstimatedCost budget = ai.getEstimatedCost();
        plan.applyAiResult(ai.getScheduleId(), budget == null ? null : budget.getTotalAmount(),
                budget == null ? null : budget.getPerPersonAmount(),
                budget == null ? null : budget.getBreakdown());

        return PlanDetailResponse.from(planRepository.save(plan));
    }

    private String title(Long memberId, PlanGenerateRequest request, String sigunguName) {
        if (request.getTitle() != null && !request.getTitle().isBlank()) return request.getTitle().strip();
        String defaultName = tourismProperties.getRegion().getChungnamName();
        String base = (sigunguName == null ? defaultName : sigunguName) + " "
                + request.getStartDate().format(DateTimeFormatter.ofPattern("MMdd"));
        if (!planRepository.existsByMemberIdAndTitle(memberId, base)) return base;
        int number = 1;
        while (planRepository.existsByMemberIdAndTitle(memberId, base + " " + number)) number++;
        return base + " " + number;
    }
}
