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

@Service
@RequiredArgsConstructor
public class TourismContentService {

    private static final int CHUNGNAM_SIGNGU_COUNT = 15;
    private static final int CONTENTS_PER_SIGNGU = 50;

    private final TourismApiClient tourismApiClient;
    private final TourismProperties tourismProperties;

    public TourismContentListResponse getContentList(
            ListType type, LocalDate visitDate, String keyword, String contentTypeId,
            String lDongSignguCd, int page, int size) {
        if (keyword != null && !keyword.isBlank()) {
            return search(keyword.trim(), contentTypeId, lDongSignguCd, page, size);
        }

        LocalDate targetDate = visitDate == null ? LocalDate.now() : visitDate;
        List<AreaBasedItem> candidates = switch (type) {
            case POPULAR -> fetchPopularCandidates();
            case SEASONAL -> fetchSeasonalCandidates(targetDate);
        };

        List<AreaBasedItem> filtered = candidates.stream()
                .filter(item -> contentTypeId == null || contentTypeId.isBlank()
                        || contentTypeId.equals(item.getContenttypeid()))
                .filter(item -> lDongSignguCd == null || lDongSignguCd.isBlank()
                        || lDongSignguCd.equals(item.getLDongSignguCd()))
                .collect(
                        LinkedHashMap<String, AreaBasedItem>::new,
                        (items, item) -> items.putIfAbsent(item.getContentid(), item),
                        LinkedHashMap::putAll
                )
                .values().stream().toList();

        return page(filtered, type, page, size);
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
                    rankScore(page * size + index),
                    List.of("검색어와 일치하는 충청남도 관광 콘텐츠입니다.")
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
            List<AreaBasedItem> candidates, ListType type, int page, int size) {
        int fromIndex = Math.min(page * size, candidates.size());
        int toIndex = Math.min(fromIndex + size, candidates.size());
        List<TourismContentDto> items = new ArrayList<>();
        for (int index = fromIndex; index < toIndex; index++) {
            String reason = type == ListType.POPULAR
                    ? "소비 강도와 체류 강도가 높은 지역의 콘텐츠입니다."
                    : "방문 예정 월의 관광 수요가 높은 지역의 콘텐츠입니다.";
            items.add(toDto(candidates.get(index), rankScore(index), List.of(reason)));
        }
        return TourismContentListResponse.builder()
                .items(items)
                .page(page)
                .size(size)
                .hasNext(toIndex < candidates.size())
                .build();
    }

    private List<AreaBasedItem> fetchPopularCandidates() {
        String chungnamCode = tourismProperties.getRegion().getChungnamCode();
        String currentYm = getCurrentYm();
        AreaTarExpDsResponse expenditure = tourismApiClient.fetchExpenditureIntensity(
                currentYm, chungnamCode, null, "2201");
        AreaTarSjrnDsResponse stay = tourismApiClient.fetchStayIntensity(
                currentYm, chungnamCode, null, "2101");

        List<String> signguCodes = calculatePopularityScore(expenditure, stay)
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(CHUNGNAM_SIGNGU_COUNT)
                .map(Map.Entry::getKey)
                .toList();
        return fetchContentsBySignguCodes(signguCodes);
    }

    private List<AreaBasedItem> fetchSeasonalCandidates(LocalDate visitDate) {
        String baseYm = visitDate.minusYears(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
        AreaTarSvcDemResponse response = tourismApiClient.fetchServiceDemand(
                baseYm, tourismProperties.getRegion().getChungnamCode(), null, "11");

        List<String> signguCodes = extractItems(response).stream()
                .sorted((a, b) -> Double.compare(
                        parseDouble(b.getTarSvcDemIxVal()),
                        parseDouble(a.getTarSvcDemIxVal())))
                .map(AreaTarSvcDemItem::getSignguCd)
                .distinct()
                .limit(CHUNGNAM_SIGNGU_COUNT)
                .toList();
        return fetchContentsBySignguCodes(signguCodes);
    }

    private List<AreaBasedItem> fetchContentsBySignguCodes(List<String> signguCodes) {
        String regionCode = tourismProperties.getRegion().getChungnamCode();
        return signguCodes.stream()
                .map(this::convertToLDongSignguCd)
                .map(signguCode -> tourismApiClient.fetchAreaBasedContent(
                        regionCode, signguCode, CONTENTS_PER_SIGNGU))
                .flatMap(List::stream)
                .toList();
    }

    private TourismContentDto toDto(
            AreaBasedItem item, BigDecimal recommendationScore, List<String> reasons) {
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
                .recommendationReasons(reasons)
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
}
