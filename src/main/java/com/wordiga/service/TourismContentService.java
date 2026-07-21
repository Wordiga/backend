package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.*;
import com.wordiga.config.TourismProperties;
import com.wordiga.dto.tourismContent.ListType;
import com.wordiga.dto.tourismContent.TourismContentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TourismContentService {

    private final TourismApiClient tourismApiClient;
    private final TourismProperties tourismProperties;

    public List<TourismContentDto> fetchListType(ListType type, String baseYm, int numOfRows) {
        return switch (type) {
            case POPULAR -> fetchPopularList(numOfRows);
            case SEASONAL -> fetchSeasonalList(baseYm, numOfRows);
        };
    }

    /**
     * 인기순: 소비강도 + 체류강도 종합 → 상위 시군구의 콘텐츠
     */
    private List<TourismContentDto> fetchPopularList(int numOfRows) {
        String chungnamCode = tourismProperties.getRegion().getChungnamCode();
        String currentYm = getCurrentYm();

        // 1. 소비 강도 (외지인 소비액 2201)
        AreaTarExpDsResponse expResponse = tourismApiClient.fetchExpenditureIntensity(
                currentYm, chungnamCode, null, "2201");

        // 2. 체류 강도 (타권역 방문자 비중 2101)
        AreaTarSjrnDsResponse sjrnResponse = tourismApiClient.fetchStayIntensity(
                currentYm, chungnamCode, null, "2101");

        // 3. 종합 점수 계산 → 상위 시군구 추출
        List<String> topSignguCodes = calculatePopularityScore(expResponse, sjrnResponse)
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(numOfRows)
                .map(Map.Entry::getKey)
                .toList();

        // 4. 상위 시군구 기반 콘텐츠 조회
        return fetchContentsBySignguCodes(topSignguCodes, numOfRows);
    }

    /**
     * 시즌 추천: 작년 동월 수요 높았던 시군구의 콘텐츠
     */
    private List<TourismContentDto> fetchSeasonalList(String baseYm, int numOfRows) {
        if (baseYm == null || baseYm.isBlank()) {
            baseYm = calculateLastYearSameMonth();
        }

        String chungnamCode = tourismProperties.getRegion().getChungnamCode();

        // 관광 서비스 수요 조회 (전체 지표 11)
        AreaTarSvcDemResponse response = tourismApiClient.fetchServiceDemand(
                baseYm, chungnamCode, null, "11");

        List<AreaTarSvcDemItem> items = extractItems(response);

        // 수요 높은 시군구 추출
        List<String> topSignguCodes = items.stream()
                .sorted((a, b) -> Double.compare(
                        Double.parseDouble(b.getTarSvcDemIxVal()),
                        Double.parseDouble(a.getTarSvcDemIxVal())))
                .map(AreaTarSvcDemItem::getSignguCd)
                .distinct()
                .limit(numOfRows)
                .toList();

        return fetchContentsBySignguCodes(topSignguCodes, numOfRows);
    }

    /**
     * 시군구 코드 기반 → 관광 콘텐츠 조회 (TourAPI 지역기반 검색 등 활용)
     */
    private List<TourismContentDto> fetchContentsBySignguCodes(List<String> signguCodes, int numOfRows) {
        String chungnamLDongRegnCd = tourismProperties.getRegion().getChungnamCode(); // "44"

        return signguCodes.stream()
                .map(signguCd -> {
                    // 수요강도 API signguCd(5자리: 44131) → 법정동 시군구코드(3자리: 131)
                    String lDongSignguCd = convertToLDongSignguCd(signguCd);

                    return tourismApiClient.fetchAreaBasedContent(
                            chungnamLDongRegnCd, lDongSignguCd, 1);
                })
                .flatMap(List::stream)
                .limit(numOfRows)
                .map(this::toDto)
                .toList();
    }

    /**
     * 수요강도 API의 signguCd(5자리: "44131") → 법정동 시군구코드(3자리: "131")
     * 앞 2자리는 시도코드이므로 제거
     */
    private String convertToLDongSignguCd(String signguCd) {
        if (signguCd == null || signguCd.length() <= 2) {
            return null;  // ← 시군구코드 없으면 null → putIfPresent에서 스킵됨
        }
        // "44131" → "131", "44230" → "230"
        return signguCd.substring(2);
    }

    private TourismContentDto toDto(AreaBasedItem item) {
        // 주소에서 시군구까지 추출 (예: "충청남도 태안군 안면읍..." → "충남 태안군")
        String location = extractLocation(item.getAddr1());

        return TourismContentDto.builder()
                .contentId(item.getContentid())
                .contentTypeId(item.getContenttypeid())
                .title(item.getTitle())
                .location(location)
                .firstimage(item.getFirstimage())
                .categoryName(mapCategoryName(item.getContenttypeid()))
                .build();
    }

    private String extractLocation(String addr1) {
        if (addr1 == null || addr1.isBlank()) return "";
        // "충청남도 태안군 안면읍 꽃지해안로 400" → "충남 태안군"
        String[] parts = addr1.split(" ");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1];
        }
        return addr1;
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

    // ─── 유틸 메서드 ───

    private String calculateLastYearSameMonth() {
        LocalDate lastYear = LocalDate.now().minusYears(1);
        return lastYear.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private String getCurrentYm() {
        LocalDate now = LocalDate.now();
        LocalDate target = now.getDayOfMonth() >= 16
                ? now.minusMonths(1)
                : now.minusMonths(2);
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
            AreaTarExpDsResponse expResponse, AreaTarSjrnDsResponse sjrnResponse) {
        Map<String, Double> scoreMap = new HashMap<>();

        extractItems(expResponse).forEach(item ->
                scoreMap.merge(item.getSignguCd(),
                        Double.parseDouble(item.getTarExpDsIxVal()) * 0.6, Double::sum));

        extractItems(sjrnResponse).forEach(item ->
                scoreMap.merge(item.getSignguCd(),
                        Double.parseDouble(item.getTarSjrnDsIxVal()) * 0.4, Double::sum));

        return scoreMap;
    }
}