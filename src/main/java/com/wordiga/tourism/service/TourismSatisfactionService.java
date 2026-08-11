package com.wordiga.tourism.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.*;
import com.wordiga.tourism.dto.SatisfactionRequestDto;
import com.wordiga.tourism.dto.detail.SatisfactionDto;
import com.wordiga.tourism.dto.detail.ScoreComponentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TourismSatisfactionService {

    private static final BigDecimal NEUTRAL_SCORE = BigDecimal.valueOf(50);
    private static final BigDecimal POPULARITY_WEIGHT = BigDecimal.valueOf(0.30);
    private static final BigDecimal AGE_FIT_WEIGHT = BigDecimal.valueOf(0.35);
    private static final BigDecimal STAY_FIT_WEIGHT = BigDecimal.valueOf(0.20);
    private static final BigDecimal COMFORT_WEIGHT = BigDecimal.valueOf(0.15);

    private final TourismApiClient tourismApiClient;

    public SatisfactionDto calculate(SatisfactionRequestDto request, CalculationCache cache) {
        if (cache == null) {
            cache = new CalculationCache();
        }

        String baseYm = latestBaseYm();
        String signguCode = request.areaCode() + request.localSignguCode();
        RegionKey regionKey = new RegionKey(baseYm, request.areaCode(), signguCode);

        // 1. 인기도 계산 (전체 지표 12, 11 사용)
        OptionalDouble popularity = cache.popularity.computeIfAbsent(regionKey,
                k -> calculatePopularity(baseYm, request.areaCode(), signguCode));

        // 2. 연령 적합도 계산 (가중치 연산)
        OptionalDouble ageFit = calculateAgeFit(baseYm, request.areaCode(), signguCode, request.ageGroupRatios());

        // 3. 체류 적합도 계산 (숙박 일수 반영)
        OptionalDouble stayFit = cache.stayFit.computeIfAbsent(
                new StayKey(regionKey, request.stayNights()),
                k -> calculateStayFit(baseYm, request.areaCode(), signguCode, request.stayNights()));

        // 4. 쾌적도 계산 (30일 관측 데이터)
        OptionalDouble comfort = calculateComfort(request.areaCode(), signguCode, request.title(), request.visitDate());

        // 모두 데이터가 없으면 null 반환
        if (popularity.isEmpty() && ageFit.isEmpty() && stayFit.isEmpty() && comfort.isEmpty()) {
            return null;
        }

        ScoreComponentDto popularityComponent = buildComponent(popularity, POPULARITY_WEIGHT, "areaCulResDemList, areaTarSvcDemList");
        ScoreComponentDto ageFitComponent = buildComponent(ageFit, AGE_FIT_WEIGHT, "areaTouDivList, areaExpDivList");
        ScoreComponentDto stayFitComponent = buildComponent(stayFit, STAY_FIT_WEIGHT, "areaTarSjrnDsList, areaTarExpDsList");
        ScoreComponentDto comfortComponent = buildComponent(comfort, COMFORT_WEIGHT, "tatsCnctrRatedList");

        BigDecimal totalScore = calculateWeightedTotal(popularityComponent, ageFitComponent, stayFitComponent, comfortComponent);

        return SatisfactionDto.builder()
                .totalScore(totalScore)
                .popularityScore(popularityComponent)
                .ageFitScore(ageFitComponent)
                .stayFitScore(stayFitComponent)
                .comfortScore(comfortComponent)
                .calculatedAt(OffsetDateTime.now())
                .build();
    }

    // ─── 1. 인기도 연산 ───
    private OptionalDouble calculatePopularity(String baseYm, String areaCode, String signguCode) {
        // 전체 지표 코드 명시 (문화: 12, 서비스: 11)
        List<AreaCulResDemItem> resources = extractItems(tourismApiClient.fetchCulturalResourceDemand(baseYm, areaCode, signguCode, "12"));
        List<AreaTarSvcDemItem> services = extractItems(tourismApiClient.fetchServiceDemand(baseYm, areaCode, signguCode, "11"));

        double resScore = resources.stream().mapToDouble(i -> parseDouble(i.getCulResDemIxVal())).filter(Double::isFinite).average().orElse(Double.NaN);
        double svcScore = services.stream().mapToDouble(i -> parseDouble(i.getTarSvcDemIxVal())).filter(Double::isFinite).average().orElse(Double.NaN);

        return averageValidValues(resScore, svcScore);
    }

    // ─── 2. 연령 적합도 연산 (가중 평균) ───
    private OptionalDouble calculateAgeFit(String baseYm, String areaCode, String signguCode, Map<String, BigDecimal> ageRatios) {
        if (CollectionUtils.isEmpty(ageRatios)) {
            return OptionalDouble.empty();
        }

        double weightedScoreSum = 0.0;
        double totalRatioApplied = 0.0;

        for (Map.Entry<String, BigDecimal> entry : ageRatios.entrySet()) {
            int age = parseAgeGroup(entry.getKey());
            if (age < 10 || age > 70) continue;

            String visitorCode = "310" + (age / 10);
            String expCode = "320" + (age / 10);
            double ratio = entry.getValue() != null ? entry.getValue().doubleValue() : 0.0;

            // 세부 지표 코드로 각각 개별 조회
            List<AreaTouDivItem> visitors = extractItems(tourismApiClient.fetchTouristDiversity(baseYm, areaCode, signguCode, visitorCode));
            List<AreaExpDivItem> expenditures = extractItems(tourismApiClient.fetchExpenditureDiversity(baseYm, areaCode, signguCode, expCode));

            double visitorVal = visitors.stream().mapToDouble(i -> parseDouble(i.getTouDivIxVal())).filter(Double::isFinite).findFirst().orElse(Double.NaN);
            double expVal = expenditures.stream().mapToDouble(i -> parseDouble(i.getExpDivIxVal())).filter(Double::isFinite).findFirst().orElse(Double.NaN);

            OptionalDouble avgAgeScore = averageValidValues(visitorVal, expVal);
            if (avgAgeScore.isPresent()) {
                weightedScoreSum += avgAgeScore.getAsDouble() * ratio;
                totalRatioApplied += ratio;
            }
        }

        return totalRatioApplied == 0 ? OptionalDouble.empty() : OptionalDouble.of(weightedScoreSum / totalRatioApplied);
    }

    // ─── 3. 체류 적합도 연산 (숙박 일수 반영) ───
    private OptionalDouble calculateStayFit(String baseYm, String areaCode, String signguCode, Integer stayNights) {
        String stayCode = resolveStayCode(stayNights);
        List<AreaTarSjrnDsItem> stayItems = extractItems(tourismApiClient.fetchStayIntensity(baseYm, areaCode, signguCode, stayCode));

        // 외지인 소비액(2201) 지표 명시적 사용
        List<AreaTarExpDsItem> expItems = extractItems(tourismApiClient.fetchExpenditureIntensity(baseYm, areaCode, signguCode, "2201"));

        double stayScore = stayItems.stream().mapToDouble(i -> parseDouble(i.getTarSjrnDsIxVal())).filter(Double::isFinite).average().orElse(Double.NaN);
        double expScore = expItems.stream().mapToDouble(i -> parseDouble(i.getTarExpDsIxVal())).filter(Double::isFinite).average().orElse(Double.NaN);

        return averageValidValues(stayScore, expScore);
    }

    // ─── 4. 쾌적도 연산 ───
    private OptionalDouble calculateComfort(String areaCode, String signguCode, String title, LocalDate visitDate) {
        List<TatsCnctrRateItem> items = extractItems(tourismApiClient.fetchConcentrationRate(areaCode, signguCode, title));
        if (items.isEmpty()) {
            return OptionalDouble.empty();
        }

        // 1. visitDate가 유효하고 30일 관측 목록 내에 존재하는지 확인
        if (visitDate != null) {
            String targetDateStr = visitDate.format(DateTimeFormatter.BASIC_ISO_DATE);
            OptionalDouble exactMatch = items.stream()
                    .filter(item -> targetDateStr.equals(item.getBaseYmd()))
                    .mapToDouble(item -> parseDouble(item.getCnctrRate()))
                    .filter(Double::isFinite)
                    .findFirst();

            if (exactMatch.isPresent()) {
                return OptionalDouble.of(clamp(100.0 - exactMatch.getAsDouble()));
            }
        }

        // 2. visitDate가 범위를 벗어났거나 지정되지 않은 경우 -> 향후 30일 전체 평균 집중률 적용
        double avgConcentration = items.stream()
                .mapToDouble(item -> parseDouble(item.getCnctrRate()))
                .filter(Double::isFinite)
                .average()
                .orElse(Double.NaN);

        return Double.isNaN(avgConcentration)
                ? OptionalDouble.empty()
                : OptionalDouble.of(clamp(100.0 - avgConcentration));
    }
    
    // ─── Helper Methods ───

    private String resolveStayCode(Integer stayNights) {
        if (stayNights == null) return "21";      // 전체
        return switch (stayNights) {
            case 0 -> "2101"; // 당일 / 타권역
            case 1 -> "2103"; // 1박
            case 2 -> "2104"; // 2박
            default -> "2105"; // 3박 이상
        };
    }

    private ScoreComponentDto buildComponent(OptionalDouble score, BigDecimal weight, String source) {
        boolean imputed = score.isEmpty();
        return ScoreComponentDto.builder()
                .score(imputed ? NEUTRAL_SCORE : BigDecimal.valueOf(clamp(score.getAsDouble())).setScale(1, RoundingMode.HALF_UP))
                .weight(weight)
                .imputed(imputed)
                .reason(imputed ? "원천 데이터가 없어 중립값 50을 적용했습니다." : null)
                .source(imputed ? null : source)
                .build();
    }

    private BigDecimal calculateWeightedTotal(ScoreComponentDto... components) {
        BigDecimal total = BigDecimal.ZERO;
        for (ScoreComponentDto comp : components) {
            total = total.add(comp.getScore().multiply(comp.getWeight()));
        }
        return total.setScale(1, RoundingMode.HALF_UP);
    }

    private OptionalDouble averageValidValues(double... values) {
        double[] valid = Arrays.stream(values).filter(Double::isFinite).toArray();
        return valid.length == 0 ? OptionalDouble.empty() : OptionalDouble.of(Arrays.stream(valid).average().orElseThrow());
    }

    private int parseAgeGroup(String ageStr) {
        if (ageStr == null) return -1;
        String digits = ageStr.replaceAll("\\D", "");
        try {
            return digits.isEmpty() ? -1 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? Double.NaN : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private String latestBaseYm() {
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

    // ─── Inner DTO / Cache ───

    public static final class CalculationCache {
        private final Map<RegionKey, OptionalDouble> popularity = new HashMap<>();
        private final Map<StayKey, OptionalDouble> stayFit = new HashMap<>();
    }

    private record RegionKey(String baseYm, String areaCode, String signguCode) {
    }

    private record StayKey(RegionKey region, Integer stayNights) {
    }
}