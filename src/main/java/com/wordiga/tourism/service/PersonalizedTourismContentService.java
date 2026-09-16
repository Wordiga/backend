package com.wordiga.tourism.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.AreaBasedItem;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.plan.service.PlanCostPolicy;
import com.wordiga.plan.dto.ContentCostDto;
import com.wordiga.tourism.dto.TourismContentDto;
import com.wordiga.tourism.dto.TourismContentListResponse;
import com.wordiga.tourism.domain.TourismCategory;
import com.wordiga.tourism.dto.CodeNameDto;
import com.wordiga.wish.service.WishPreferenceCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.wordiga.global.util.KtoUtils.parseBigDecimal;

@Service
@RequiredArgsConstructor
public class PersonalizedTourismContentService {

    private static final int TOTAL_CANDIDATE_LIMIT = 50;
    private static final List<String> DEFAULT_FALLBACK_SIGUNGUS = List.of("131", "200", "340");

    private final TourismApiClient tourismApiClient;
    private final WishPreferenceCacheService preferenceCacheService;
    private final TourismProperties tourismProperties;
    private final TourismSatisfactionService satisfactionService;
    private final TourismContentDetailService detailService;

    public TourismContentListResponse get(
            Long memberId, LocalDate visitDate, List<String> ageGroups, Integer participantCount,
            Boolean capacitySatisfied, String theme, List<String> categories, int page, int size) {

        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 1. 인메모리 캐시에서 유저의 시군구별 비중(Map) 조회
        Map<String, BigDecimal> sigunguRatios = preferenceCacheService.getSigunguPreferenceRatios(memberId);
        Map<String, BigDecimal> categoryRatios = preferenceCacheService.getCategoryPreferenceRatios(memberId);

        String regionCode = tourismProperties.getRegion().getChungnamCode();
        Map<String, TourismContentDto> candidateMap = new LinkedHashMap<>();
        Map<String, BigDecimal> ageRatios = TourismContentDetailService.parseAgeRatios(ageGroups);
        Map<String, BigDecimal> ageScores = new HashMap<>();
        int participants = participantCount == null ? 10 : participantCount;

        // 2-A. 위시 비중 데이터가 없는 신규/미등록 회원 -> Fallback 대표 시군구 조회
        if (sigunguRatios.isEmpty()) {
            fetchCandidatesForSigungus(
                    DEFAULT_FALLBACK_SIGUNGUS, regionCode, candidateMap, sigunguRatios, categoryRatios,
                    ageRatios, ageScores, visitDate, participants, 15
            );
        }
        // 2-B. 위시 비중 데이터가 있는 회원 -> 지분율(Ratio)에 비례하여 시군구별 API 호출 제한 조율
        else {
            for (Map.Entry<String, BigDecimal> entry : sigunguRatios.entrySet()) {
                String sigunguCode = entry.getKey();
                BigDecimal ratio = entry.getValue();

                // 비율에 비례하여 후보군 수집 제한 개수 계산 (최소 5개 보장)
                int fetchLimit = Math.max(ratio.multiply(BigDecimal.valueOf(TOTAL_CANDIDATE_LIMIT)).intValue(), 5);

                fetchCandidatesForSigungus(
                        List.of(sigunguCode), regionCode, candidateMap, sigunguRatios, categoryRatios,
                        ageRatios, ageScores, visitDate, participants, fetchLimit
                );

                if (candidateMap.size() >= TOTAL_CANDIDATE_LIMIT) break;
            }
        }

        // 3. 만족도 점수 기준 내림차순 정렬 및 메모리 페이징
        List<TourismContentDto> sortedList = candidateMap.values().stream()
                .filter(item -> !"15".equals(item.getContentTypeId()))
                .filter(item -> theme == null || theme.isBlank()
                        || item.getTheme() != null && theme.equals(item.getTheme().code()))
                .filter(item -> categories == null || categories.isEmpty() || item.getCategory() != null
                        && categories.stream().filter(java.util.Objects::nonNull)
                        .flatMap(value -> java.util.Arrays.stream(value.split(","))).map(String::trim)
                        .anyMatch(item.getCategory().code()::equals))
                .filter(item -> eligibleForParticipants(item, participants, capacitySatisfied))
                .sorted(Comparator.comparing(TourismContentDto::getRecommendationScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparingInt(item -> Objects.hash(LocalDate.now(), item.getContentId())))
                .toList();

        int fromIndex = Math.min(page * size, sortedList.size());
        int toIndex = Math.min(fromIndex + size, sortedList.size());

        return TourismContentListResponse.builder()
                .items(sortedList.subList(fromIndex, toIndex))
                .page(page)
                .size(size)
                .totalCount(sortedList.size())
                .totalPages((sortedList.size() + size - 1) / size)
                .hasNext(toIndex < sortedList.size())
                .build();
    }

    private boolean eligibleForParticipants(TourismContentDto item, int participants,
                                            Boolean capacitySatisfied) {
        if (!"32".equals(item.getContentTypeId())) return !Boolean.TRUE.equals(capacitySatisfied);
        Boolean eligible = detailService.capacitySatisfied(item.getContentId(), participants);
        return Boolean.TRUE.equals(capacitySatisfied) ? Boolean.TRUE.equals(eligible)
                : !Boolean.FALSE.equals(eligible);
    }

    private void fetchCandidatesForSigungus(
            List<String> sigungus, String regionCode, Map<String, TourismContentDto> candidateMap,
            Map<String, BigDecimal> sigunguRatios, Map<String, BigDecimal> categoryRatios,
            Map<String, BigDecimal> ageRatios, Map<String, BigDecimal> ageScores,
            LocalDate visitDate, int participantCount, int limitPerSigungu) {

        for (String sigungu : sigungus) {
            List<AreaBasedItem> items = tourismApiClient.fetchAreaBasedContent(regionCode, sigungu, limitPerSigungu);

            for (AreaBasedItem item : items) {
                if (candidateMap.containsKey(item.getContentid())) continue;

                BigDecimal ageScore = ageScores.computeIfAbsent(sigungu,
                        code -> satisfactionService.ageFitForRegion(regionCode, code, ageRatios));
                BigDecimal score = personalizedScore(item, sigunguRatios, categoryRatios, visitDate, ageScore);
                candidateMap.put(item.getContentid(), buildContentDto(item, score, participantCount));
            }
        }
    }
    // ─── Helper Methods ───

    private BigDecimal personalizedScore(AreaBasedItem item, Map<String, BigDecimal> sigunguRatios,
                                         Map<String, BigDecimal> categoryRatios, LocalDate visitDate,
                                         BigDecimal ageScore) {
        BigDecimal region = sigunguRatios.isEmpty() ? BigDecimal.valueOf(50)
                : sigunguRatios.getOrDefault(item.getLDongSignguCd(), BigDecimal.ZERO).movePointRight(2);
        BigDecimal category = categoryRatios.isEmpty() ? BigDecimal.valueOf(50)
                : categoryRatios.getOrDefault(item.getLclsSystm1(), BigDecimal.ZERO).movePointRight(2);
        int month = visitDate == null ? LocalDate.now().getMonthValue() : visitDate.getMonthValue();
        BigDecimal seasonal = BigDecimal.valueOf(seasonalFit(item.getContenttypeid(), month));
        return region.multiply(BigDecimal.valueOf(0.4)).add(category.multiply(BigDecimal.valueOf(0.25)))
                .add(seasonal.multiply(BigDecimal.valueOf(0.2)))
                .add(ageScore.multiply(BigDecimal.valueOf(0.15))).setScale(1, RoundingMode.HALF_UP);
    }

    private int seasonalFit(String type, int month) {
        if (List.of(3, 4, 5).contains(month)) return List.of("12", "25").contains(type) ? 100 : 50;
        if (List.of(6, 7, 8).contains(month)) return List.of("12", "28").contains(type) ? 100 : 50;
        if (List.of(9, 10, 11).contains(month)) return List.of("12", "14", "25").contains(type) ? 100 : 50;
        return List.of("14", "28", "32").contains(type) ? 100 : 50;
    }

    private TourismContentDto buildContentDto(AreaBasedItem item, BigDecimal score, int participantCount) {
        TourismCategory category = TourismCategory.resolve(
                item.getLclsSystm1(), item.getLclsSystm2(), item.getLclsSystm3());
        return TourismContentDto.builder()
                .contentId(item.getContentid())
                .contentTypeId(item.getContenttypeid())
                .title(item.getTitle())
                .addr1(item.getAddr1())
                .firstImage(item.getFirstimage())
                .lDongSignguCd(item.getLDongSignguCd())
                .mapx(parseBigDecimal(item.getMapx()))
                .mapy(parseBigDecimal(item.getMapy()))
                .theme(category == null ? null : new CodeNameDto(
                        category.getTheme().getCode(), category.getTheme().getDisplayName()))
                .category(category == null ? null : new CodeNameDto(category.getCode(), category.getDisplayName()))
                .recommendationScore(score)
                .estimatedCost(PlanCostPolicy.defaultPerPersonAmount(item.getContenttypeid(), participantCount))
                .cost(ContentCostDto.from(PlanCostPolicy.defaultEstimate(item.getContenttypeid(), participantCount),
                        participantCount))
                .build();
    }

}
