package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.AreaBasedItem;
import com.wordiga.client.dto.AreaBasedResponse;
import com.wordiga.client.dto.AreaTarExpDsResponse;
import com.wordiga.client.dto.AreaTarSjrnDsResponse;
import com.wordiga.client.dto.AreaTarSvcDemItem;
import com.wordiga.client.dto.AreaTarSvcDemResponse;
import com.wordiga.client.dto.KtoApiResponse;
import com.wordiga.config.TourismProperties;
import com.wordiga.dto.tourismContent.ListType;
import com.wordiga.dto.tourismContent.TourismContentDto;
import com.wordiga.dto.tourismContent.TourismContentListResponse;
import com.wordiga.repository.WishRepository;
import com.wordiga.wish.Wish;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourismContentService {

    private static final int CHUNGNAM_SIGNGU_COUNT = 15;
    private static final int CONTENTS_PER_SIGNGU = 50;

    private final TourismApiClient tourismApiClient;
    private final TourismProperties tourismProperties;
    private final RelatedTourismContentService relatedTourismContentService;
    private final PersonalizedTourismContentService personalizedTourismContentService;
    private final TourismContentDetailService detailService;
    private final WishRepository wishRepository;

    public TourismContentListResponse getContentList(
            ListType type, LocalDate visitDate, String keyword, String contentTypeId,
            String lDongSignguCd, int page, int size) {
        return getContentList(null, type, visitDate, keyword, contentTypeId, lDongSignguCd,
                null, null, null, null, page, size);
    }

    public TourismContentListResponse getContentList(
            Long memberId, ListType type, LocalDate visitDate, String keyword, String contentTypeId,
            String lDongSignguCd, String referenceContentId, Boolean capacitySatisfied,
            Integer participantCount, List<String> ageGroups, int page, int size) {
        if (type == ListType.RELATED) {
            if (referenceContentId == null || referenceContentId.isBlank())
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "연관 추천 기준 콘텐츠 ID가 필요합니다.");
            return markWished(memberId,
                    relatedTourismContentService.get(referenceContentId, visitDate, ageGroups, page, size));
        }
        if (type == ListType.PERSONALIZED)
            return markWished(memberId, personalizedTourismContentService.get(memberId, visitDate, page, size));
        if (keyword != null && !keyword.isBlank()) {
            return markWished(memberId,
                    filterCapacity(search(keyword.trim(), contentTypeId, lDongSignguCd, page, size),
                            capacitySatisfied, participantCount));
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

        return markWished(memberId,
                filterCapacity(page(filtered, type, page, size), capacitySatisfied, participantCount));
    }

    private TourismContentListResponse markWished(Long memberId, TourismContentListResponse response) {
        Set<String> wishedContentIds = memberId == null ? Set.of()
                : wishRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
                  .map(Wish::getContentId)
                  .collect(Collectors.toSet());
        response.getItems().forEach(item -> item.setWished(wishedContentIds.contains(item.getContentId())));
        return response;
    }

    private TourismContentListResponse filterCapacity(TourismContentListResponse response,
                                                       Boolean required, Integer participantCount) {
        if (!Boolean.TRUE.equals(required)) return response;
        if (participantCount == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "수용 가능 숙소 필터에는 참가 인원이 필요합니다.");
        List<TourismContentDto> items = response.getItems().stream()
                .filter(item -> "32".equals(item.getContentTypeId()))
                .filter(item -> Boolean.TRUE.equals(detailService.capacitySatisfied(
                        item.getContentId(), item.getContentTypeId(), participantCount)))
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
                currentYm, chungnamCode, null, "2101");

        Map<String, Double> scores = calculatePopularityScore(expenditure, stay);
        List<String> signguCodes = scores
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(CHUNGNAM_SIGNGU_COUNT)
                .map(Map.Entry::getKey)
                .toList();
        return fetchContentsBySignguCodes(signguCodes, scores);
    }

    private List<ScoredCandidate> fetchSeasonalCandidates(LocalDate visitDate) {
        String baseYm = visitDate.minusYears(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
        AreaTarSvcDemResponse response = tourismApiClient.fetchServiceDemand(
                baseYm, tourismProperties.getRegion().getChungnamCode(), null, "11");

        Map<String, Double> scores = extractItems(response).stream().collect(java.util.stream.Collectors.toMap(
                AreaTarSvcDemItem::getSignguCd, item -> parseDouble(item.getTarSvcDemIxVal()), Math::max));
        List<String> signguCodes = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(CHUNGNAM_SIGNGU_COUNT)
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
                .mapx(toBigDecimal(item.getMapx()))
                .mapy(toBigDecimal(item.getMapy()))
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

    private BigDecimal toBigDecimal(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String mapCategoryName(String contentTypeId) {
        if (contentTypeId == null) return "기타";
        return switch (contentTypeId) {
            case "12" -> "관광지";
            case "14" -> "문화시설";
            case "15" -> "행사/공연/축제";
            case "25" -> "여행코스";
            case "28" -> "레포츠";
            case "32" -> "숙박";
            case "38" -> "쇼핑";
            case "39" -> "음식점";
            default -> "기타";
        };
    }

    private String getCurrentYm() {
        LocalDate now = LocalDate.now();
        LocalDate target = now.getDayOfMonth() >= 16 ? now.minusMonths(1) : now.minusMonths(2);
        return target.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private <T> List<T> extractItems(KtoApiResponse<T> response) {
        if (response == null || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody().getItems().getItem() == null) {
            return Collections.emptyList();
        }
        return response.getResponse().getBody().getItems().getItem();
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

    private double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private record ScoredCandidate(AreaBasedItem item, BigDecimal score) { }
}
