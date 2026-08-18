package com.wordiga.tourism.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.*;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.wish.repository.WishRepository;
import com.wordiga.tourism.domain.TourismContentType;
import com.wordiga.tourism.dto.ListType;
import com.wordiga.tourism.dto.SatisfactionRequestDto;
import com.wordiga.tourism.dto.SigunguResponse;
import com.wordiga.tourism.dto.TourismContentDto;
import com.wordiga.tourism.dto.TourismContentListResponse;
import com.wordiga.wish.Wish;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.wordiga.global.util.KtoUtils.*;

@Service
@RequiredArgsConstructor
public class TourismContentService {

    private static final int CONTENTS_PER_SIGNGU = 50;
    private static final int DEFAULT_PARTICIPANT_COUNT = 10;

    private final TourismApiClient tourismApiClient;
    private final TourismProperties tourismProperties;
    private final PersonalizedTourismContentService personalizedTourismContentService;
    private final TourismContentDetailService detailService;
    private final TourismSatisfactionService satisfactionService;
    private final WishRepository wishRepository;

    public List<SigunguResponse> getSigunguList() {
        List<SigunguItem> items = tourismApiClient.fetchSigunguList();
        return items.stream()
                .map(item -> new SigunguResponse(item.getCode(), item.getName()))
                .toList();
    }

    public TourismContentListResponse getContentList(
            Long memberId, ListType type, LocalDate visitDate, String keyword, String contentTypeId,
            String lDongSignguCd, Boolean capacitySatisfied,
            Integer participantCount, List<String> ageGroups, int page, int size) {
        if (type == ListType.PERSONALIZED)
            return personalizedTourismContentService.get(memberId, visitDate, ageGroups, null, page, size);
        if (keyword != null && !keyword.isBlank()) {
            return enrichMemberData(memberId, visitDate, ageGroups,
                    filterCapacity(search(keyword.trim(), contentTypeId, lDongSignguCd, page, size),
                            capacitySatisfied, participantCount, visitDate, ageGroups));
        }

        LocalDate targetDate = visitDate == null ? LocalDate.now() : visitDate;
        List<ScoredCandidate> candidates = switch (type) {
            case POPULAR -> fetchPopularCandidates();
            case SEASONAL -> fetchSeasonalCandidates(targetDate);
            default -> throw new IllegalStateException("지원하지 않는 추천 타입입니다.");
        };

        List<ScoredCandidate> filtered = candidates.stream()
                .filter(candidate -> contentTypeId == null || contentTypeId.isBlank()
                        || contentTypeId.equals(candidate.item().getContenttypeid()))
                .filter(candidate -> lDongSignguCd == null || lDongSignguCd.isBlank()
                        || lDongSignguCd.equals(candidate.item().getLDongSignguCd()))
                .collect(
                        LinkedHashMap<String, ScoredCandidate>::new,
                        (items, candidate) -> items.putIfAbsent(candidate.item().getContentid(), candidate),
                        LinkedHashMap::putAll
                )
                .values().stream().toList();

        return enrichMemberData(memberId, visitDate, ageGroups,
                filterCapacity(page(filtered, type, page, size), capacitySatisfied, participantCount,
                        visitDate, ageGroups));
    }

    private TourismContentListResponse enrichMemberData(
            Long memberId, LocalDate visitDate, List<String> ageGroups, TourismContentListResponse response) {
        Set<String> wishedContentIds = memberId == null ? Set.of()
                : wishRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
                  .map(Wish::getContentId)
                  .collect(Collectors.toSet());
        var satisfactionCache = new TourismSatisfactionService.CalculationCache();
        String regionCode = tourismProperties.getRegion().getChungnamCode();
        response.getItems().forEach(item -> {
            item.setWished(wishedContentIds.contains(item.getContentId()));
            if (memberId != null && item.getLDongSignguCd() != null && item.getTitle() != null) {
                item.setSatisfaction(satisfactionService.calculate(new SatisfactionRequestDto(
                        regionCode, item.getLDongSignguCd(), item.getTitle(), visitDate,
                        ageGroups == null ? Map.of() : ageGroups.stream().filter(Objects::nonNull).distinct()
                                .collect(Collectors.toMap(age -> age, age -> BigDecimal.ONE)), null), satisfactionCache));
            }
        });
        return response;
    }

    private TourismContentListResponse filterCapacity(TourismContentListResponse response,
                                                      Boolean required, Integer participantCount,
                                                      LocalDate visitDate, List<String> ageGroups) {
        if (!Boolean.TRUE.equals(required)) return response;
        int participants = participantCount == null ? DEFAULT_PARTICIPANT_COUNT : participantCount;
        List<TourismContentDto> items = response.getItems().stream()
                .filter(item -> TourismContentType.LODGING.getCode().equals(item.getContentTypeId()))
                .filter(item -> Boolean.TRUE.equals(detailService.getDetail(
                        item.getContentId(), visitDate, ageGroups, null, participants).getCapacitySatisfied()))
                .toList();
        return TourismContentListResponse.builder().items(items).page(response.getPage()).size(response.getSize())
                .hasNext(response.isHasNext()).build();
    }

    private TourismContentListResponse search(
            String keyword, String contentTypeId, String lDongSignguCd, int page, int size) {
        AreaBasedResponse response = tourismApiClient.searchContent(
                keyword,
                contentTypeId,
                tourismProperties.getRegion().getChungnamCode(),
                lDongSignguCd,
                page + 1,
                size
        );
        List<AreaBasedItem> items = extractItems(response);
        int totalCount = response == null || response.getResponse() == null
                || response.getResponse().getBody() == null
                ? items.size()
                : response.getResponse().getBody().getTotalCount();

        List<TourismContentDto> result = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            result.add(toDto(
                    items.get(index),
                    rankScore(page * size + index)
            ));
        }
        return TourismContentListResponse.builder()
                .items(result)
                .page(page)
                .size(size)
                .hasNext((long) (page + 1) * size < totalCount)
                .build();
    }

    private TourismContentListResponse page(
            List<ScoredCandidate> candidates, ListType type, int page, int size) {
        int fromIndex = Math.min(page * size, candidates.size());
        int toIndex = Math.min(fromIndex + size, candidates.size());
        List<TourismContentDto> items = new ArrayList<>();
        for (int index = fromIndex; index < toIndex; index++) {
            ScoredCandidate candidate = candidates.get(index);
            items.add(toDto(candidate.item(), candidate.score()));
        }
        return TourismContentListResponse.builder()
                .items(items)
                .page(page)
                .size(size)
                .hasNext(toIndex < candidates.size())
                .build();
    }

    private List<ScoredCandidate> fetchPopularCandidates() {
        String chungnamCode = tourismProperties.getRegion().getChungnamCode();
        String currentYm = getCurrentYm();
        AreaTarExpDsResponse expenditure = tourismApiClient.fetchExpenditureIntensity(
                currentYm, chungnamCode, null, "2201");
        AreaTarSjrnDsResponse stay = tourismApiClient.fetchStayIntensity(
                currentYm, chungnamCode, null, "2103");

        Map<String, Double> scores = calculatePopularityScore(expenditure, stay);
        List<String> signguCodes = scores
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        return fetchContentsBySignguCodes(signguCodes, scores);
    }

    private List<ScoredCandidate> fetchSeasonalCandidates(LocalDate visitDate) {
        String baseYm = visitDate.minusYears(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
        AreaTarSvcDemResponse response = tourismApiClient.fetchServiceDemand(
                baseYm, tourismProperties.getRegion().getChungnamCode(), null, "11");

        Map<String, Double> scores = extractItems(response).stream().collect(Collectors.toMap(
                AreaTarSvcDemItem::getSignguCd, item -> parseDouble(item.getTarSvcDemIxVal()), Math::max));
        List<String> signguCodes = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        return fetchContentsBySignguCodes(signguCodes, scores);
    }

    private List<ScoredCandidate> fetchContentsBySignguCodes(List<String> signguCodes, Map<String, Double> scores) {
        String regionCode = tourismProperties.getRegion().getChungnamCode();
        return signguCodes.stream()
                .map(this::convertToLDongSignguCd)
                .flatMap(signguCode -> tourismApiClient.fetchAreaBasedContent(
                                regionCode, signguCode, CONTENTS_PER_SIGNGU).stream()
                        .map(item -> new ScoredCandidate(item,
                                BigDecimal.valueOf(scores.getOrDefault(regionCode + signguCode, 0D)))))
                .toList();
    }

    private TourismContentDto toDto(
            AreaBasedItem item, BigDecimal recommendationScore) {
        return TourismContentDto.builder()
                .contentId(item.getContentid())
                .contentTypeId(item.getContenttypeid())
                .title(item.getTitle())
                .addr1(item.getAddr1())
                .lDongSignguCd(item.getLDongSignguCd())
                .mapx(parseBigDecimal(item.getMapx()))
                .mapy(parseBigDecimal(item.getMapy()))
                .firstImage(item.getFirstimage())
                .categoryName(mapCategoryName(item.getContenttypeid()))
                .recommendationScore(recommendationScore)
                .build();
    }

    private String convertToLDongSignguCd(String signguCd) {
        return signguCd == null || signguCd.length() <= 2 ? null : signguCd.substring(2);
    }

    private BigDecimal rankScore(int index) {
        return BigDecimal.valueOf(Math.max(1, 100 - index));
    }

    private String mapCategoryName(String contentTypeId) {
        return TourismContentType.fromCode(contentTypeId).getDisplayName();
    }

    private String getCurrentYm() {
        LocalDate now = LocalDate.now();
        LocalDate target = now.getDayOfMonth() >= 16 ? now.minusMonths(1) : now.minusMonths(2);
        return target.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private Map<String, Double> calculatePopularityScore(
            AreaTarExpDsResponse expenditure, AreaTarSjrnDsResponse stay) {
        Map<String, Double> scores = new HashMap<>();
        extractItems(expenditure).forEach(item ->
                scores.merge(item.getSignguCd(), parseDouble(item.getTarExpDsIxVal()) * 0.6, Double::sum));
        extractItems(stay).forEach(item ->
                scores.merge(item.getSignguCd(), parseDouble(item.getTarSjrnDsIxVal()) * 0.4, Double::sum));
        return scores;
    }

    private record ScoredCandidate(AreaBasedItem item, BigDecimal score) {
    }
}
