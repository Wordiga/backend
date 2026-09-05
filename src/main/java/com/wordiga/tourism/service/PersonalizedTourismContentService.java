package com.wordiga.tourism.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.AreaBasedItem;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.tourism.dto.SatisfactionRequestDto;
import com.wordiga.tourism.dto.TourismContentDto;
import com.wordiga.tourism.dto.TourismContentListResponse;
import com.wordiga.tourism.dto.detail.SatisfactionDto;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.wordiga.global.util.KtoUtils.parseBigDecimal;

@Service
@RequiredArgsConstructor
public class PersonalizedTourismContentService {

    private static final int TOTAL_CANDIDATE_LIMIT = 50;
    private static final List<String> DEFAULT_FALLBACK_SIGUNGUS = List.of("150", "200", "010");

    private final TourismApiClient tourismApiClient;
    private final WishPreferenceCacheService preferenceCacheService;
    private final TourismSatisfactionService satisfactionService;
    private final TourismProperties tourismProperties;

    public TourismContentListResponse get(
            Long memberId, LocalDate visitDate, List<String> ageGroups, Integer stayNights, int page, int size) {

        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 1. 인메모리 캐시에서 유저의 시군구별 비중(Map) 조회
        Map<String, BigDecimal> sigunguRatios = preferenceCacheService.getSigunguPreferenceRatios(memberId);

        String regionCode = tourismProperties.getRegion().getChungnamCode();
        Map<String, TourismContentDto> candidateMap = new LinkedHashMap<>();
        var cache = new TourismSatisfactionService.CalculationCache();

        // 2-A. 위시 비중 데이터가 없는 신규/미등록 회원 -> Fallback 대표 시군구 조회
        if (sigunguRatios.isEmpty()) {
            fetchCandidatesForSigungus(
                    DEFAULT_FALLBACK_SIGUNGUS, regionCode, candidateMap, cache, visitDate, ageGroups, stayNights, 15
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
                        List.of(sigunguCode), regionCode, candidateMap, cache, visitDate, ageGroups, stayNights, fetchLimit
                );

                if (candidateMap.size() >= TOTAL_CANDIDATE_LIMIT) break;
            }
        }

        // 3. 만족도 점수 기준 내림차순 정렬 및 메모리 페이징
        List<TourismContentDto> sortedList = candidateMap.values().stream()
                .sorted(Comparator.comparing(TourismContentDto::getRecommendationScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int fromIndex = Math.min(page * size, sortedList.size());
        int toIndex = Math.min(fromIndex + size, sortedList.size());

        return TourismContentListResponse.builder()
                .items(sortedList.subList(fromIndex, toIndex))
                .page(page)
                .size(size)
                .hasNext(toIndex < sortedList.size())
                .build();
    }

    private void fetchCandidatesForSigungus(
            List<String> sigungus, String regionCode, Map<String, TourismContentDto> candidateMap,
            TourismSatisfactionService.CalculationCache cache, LocalDate visitDate,
            List<String> ageGroups, Integer stayNights, int limitPerSigungu) {

        for (String sigungu : sigungus) {
            List<AreaBasedItem> items = tourismApiClient.fetchAreaBasedContent(regionCode, sigungu, limitPerSigungu);

            for (AreaBasedItem item : items) {
                if (candidateMap.containsKey(item.getContentid())) continue;

                var request = new SatisfactionRequestDto(
                        regionCode, item.getLDongSignguCd(), item.getTitle(), visitDate, parseAgeRatios(ageGroups), stayNights
                );

                SatisfactionDto satisfaction = satisfactionService.calculate(request, cache);
                BigDecimal score = (satisfaction != null) ? satisfaction.getTotalScore() : BigDecimal.ZERO;

                candidateMap.put(item.getContentid(), buildContentDto(item, score, satisfaction));
            }
        }
    }
    // ─── Helper Methods ───

    private TourismContentDto buildContentDto(AreaBasedItem item, BigDecimal score, SatisfactionDto satisfaction) {
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
                .satisfaction(satisfaction)
                .build();
    }

    private Map<String, BigDecimal> parseAgeRatios(List<String> ageGroups) {
        if (ageGroups == null || ageGroups.isEmpty()) return Map.of();
        BigDecimal equalRatio = BigDecimal.ONE.divide(BigDecimal.valueOf(ageGroups.size()), 2, RoundingMode.HALF_UP);
        return ageGroups.stream().collect(Collectors.toMap(age -> age, age -> equalRatio, (a, b) -> a));
    }

}
