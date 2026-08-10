package com.wordiga.tourism.service;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.tourism.dto.detail.SatisfactionDto;
import com.wordiga.tourism.dto.detail.ScoreComponentDto;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.DoubleStream;

@Service
@RequiredArgsConstructor
public class TourismSatisfactionService {

    private static final BigDecimal NEUTRAL_SCORE = BigDecimal.valueOf(50);
    private static final BigDecimal POPULARITY_WEIGHT = BigDecimal.valueOf(0.30);
    private static final BigDecimal AGE_FIT_WEIGHT = BigDecimal.valueOf(0.35);
    private static final BigDecimal STAY_FIT_WEIGHT = BigDecimal.valueOf(0.20);
    private static final BigDecimal COMFORT_WEIGHT = BigDecimal.valueOf(0.15);

    private final TourismApiClient tourismApiClient;

    public SatisfactionDto calculate(
            ContentDetailDto content, LocalDate visitDate, List<String> ageGroups) {
        return calculate(new Context(), content.getLDongRegnCd(), content.getLDongSignguCd(),
                content.getTitle(), visitDate, ageGroups);
    }

    public SatisfactionDto calculate(Context context, String areaCode, String localSignguCode,
                                     String title, LocalDate visitDate, List<String> ageGroups) {
        String baseYm = latestBaseYm();
        String signguCode = areaCode + localSignguCode;
        RegionKey region = new RegionKey(baseYm, areaCode, signguCode);

        OptionalDouble popularity = context.popularity.computeIfAbsent(region,
                ignored -> popularity(baseYm, areaCode, signguCode));
        List<String> normalizedAges = ageGroups == null ? List.of()
                : ageGroups.stream().filter(java.util.Objects::nonNull).toList();
        AgeKey ageKey = new AgeKey(region, normalizedAges);
        OptionalDouble ageFit = context.ageFit.computeIfAbsent(ageKey,
                ignored -> ageFit(baseYm, areaCode, signguCode, normalizedAges));
        OptionalDouble stayFit = context.stayFit.computeIfAbsent(region,
                ignored -> stayFit(baseYm, areaCode, signguCode));
        OptionalDouble comfort = comfort(areaCode, signguCode, title, visitDate);
        if (popularity.isEmpty() && ageFit.isEmpty() && stayFit.isEmpty() && comfort.isEmpty()) {
            return null;
        }

        ScoreComponentDto popularityComponent = component(
                popularity, POPULARITY_WEIGHT, "areaCulResDemList, areaTarSvcDemList");
        ScoreComponentDto ageFitComponent = component(
                ageFit, AGE_FIT_WEIGHT, "areaTouDivList, areaExpDivList");
        ScoreComponentDto stayFitComponent = component(
                stayFit, STAY_FIT_WEIGHT, "areaTarSjrnDsList, areaTarExpDsList");
        ScoreComponentDto comfortComponent = component(
                comfort, COMFORT_WEIGHT, "tatsCnctrRateList");

        BigDecimal total = weighted(popularityComponent)
                .add(weighted(ageFitComponent))
                .add(weighted(stayFitComponent))
                .add(weighted(comfortComponent))
                .setScale(1, RoundingMode.HALF_UP);
        return SatisfactionDto.builder()
                .totalScore(total)
                .popularityScore(popularityComponent)
                .ageFitScore(ageFitComponent)
                .stayFitScore(stayFitComponent)
                .comfortScore(comfortComponent)
                .calculatedAt(OffsetDateTime.now())
                .build();
    }

    private OptionalDouble popularity(String baseYm, String areaCode, String signguCode) {
        List<AreaCulResDemItem> resources = extractItems(tourismApiClient.fetchCulturalResourceDemand(
                baseYm, areaCode, signguCode, null));
        List<AreaTarSvcDemItem> services = extractItems(tourismApiClient.fetchServiceDemand(
                baseYm, areaCode, signguCode, null));
        return average(
                resources.stream().mapToDouble(item -> parse(item.getCulResDemIxVal())),
                services.stream().mapToDouble(item -> parse(item.getTarSvcDemIxVal()))
        );
    }

    private OptionalDouble ageFit(
            String baseYm, String areaCode, String signguCode, List<String> ageGroups) {
        if (ageGroups == null || ageGroups.isEmpty()) {
            return OptionalDouble.empty();
        }
        Set<String> touristCodes = ageGroups.stream()
                .map(this::ageNumber)
                .filter(age -> age >= 10 && age <= 70)
                .map(age -> "310" + age / 10)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> expenditureCodes = ageGroups.stream()
                .map(this::ageNumber)
                .filter(age -> age >= 10 && age <= 70)
                .map(age -> "320" + age / 10)
                .collect(java.util.stream.Collectors.toSet());

        AreaTouDivResponse touristResponse = tourismApiClient.fetchTouristDiversity(
                baseYm, areaCode, signguCode, null);
        AreaExpDivResponse expenditureResponse = tourismApiClient.fetchExpenditureDiversity(
                baseYm, areaCode, signguCode, null);
        DoubleStream tourists = extractItems(touristResponse).stream()
                .filter(item -> touristCodes.contains(item.getTouDivIxCd()))
                .mapToDouble(item -> parse(item.getTouDivIxVal()));
        DoubleStream expenditures = extractItems(expenditureResponse).stream()
                .filter(item -> expenditureCodes.contains(item.getExpDivIxCd()))
                .mapToDouble(item -> parse(item.getExpDivIxVal()));
        return average(tourists, expenditures);
    }

    private OptionalDouble stayFit(String baseYm, String areaCode, String signguCode) {
        List<AreaTarSjrnDsItem> stay = extractItems(tourismApiClient.fetchStayIntensity(
                baseYm, areaCode, signguCode, "2101"));
        List<AreaTarExpDsItem> expenditure = extractItems(tourismApiClient.fetchExpenditureIntensity(
                baseYm, areaCode, signguCode, "2201"));
        return average(
                stay.stream().mapToDouble(item -> parse(item.getTarSjrnDsIxVal())),
                expenditure.stream().mapToDouble(item -> parse(item.getTarExpDsIxVal()))
        );
    }

    private OptionalDouble comfort(
            String areaCode, String signguCode, String title, LocalDate visitDate) {
        List<TatsCnctrRateItem> items = extractItems(tourismApiClient.fetchConcentrationRate(
                areaCode, signguCode, title));
        if (items.isEmpty()) {
            return OptionalDouble.empty();
        }
        String targetDate = (visitDate == null ? LocalDate.now() : visitDate)
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        TatsCnctrRateItem selected = items.stream()
                .filter(item -> targetDate.equals(item.getBaseYmd()))
                .findFirst()
                .orElse(items.getFirst());
        return numeric(selected.getCnctrRate()).stream()
                .map(value -> clamp(100 - value))
                .findFirst();
    }

    private ScoreComponentDto component(
            OptionalDouble score, BigDecimal weight, String source) {
        boolean imputed = score.isEmpty();
        return ScoreComponentDto.builder()
                .score(imputed
                        ? NEUTRAL_SCORE
                        : BigDecimal.valueOf(clamp(score.getAsDouble())).setScale(1, RoundingMode.HALF_UP))
                .weight(weight)
                .imputed(imputed)
                .reason(imputed ? "원천 데이터가 없어 중립값 50을 적용했습니다." : null)
                .source(imputed ? null : source)
                .build();
    }

    private BigDecimal weighted(ScoreComponentDto component) {
        return component.getScore().multiply(component.getWeight());
    }

    private OptionalDouble average(DoubleStream first, DoubleStream second) {
        double[] values = DoubleStream.concat(first, second)
                .filter(Double::isFinite)
                .toArray();
        return values.length == 0
                ? OptionalDouble.empty()
                : OptionalDouble.of(DoubleStream.of(values).average().orElseThrow());
    }

    private int ageNumber(String value) {
        if (value == null) {
            return -1;
        }
        String digits = value.replaceAll("\\D", "");
        try {
            return digits.isEmpty() ? -1 : Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private double parse(String value) {
        return numeric(value).orElse(Double.NaN);
    }

    private OptionalDouble numeric(String value) {
        try {
            return value == null || value.isBlank()
                    ? OptionalDouble.empty()
                    : OptionalDouble.of(Double.parseDouble(value));
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
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

    public static final class Context {
        private final Map<RegionKey, OptionalDouble> popularity = new HashMap<>();
        private final Map<AgeKey, OptionalDouble> ageFit = new HashMap<>();
        private final Map<RegionKey, OptionalDouble> stayFit = new HashMap<>();
    }

    private record RegionKey(String baseYm, String areaCode, String signguCode) {
    }

    private record AgeKey(RegionKey region, List<String> ageGroups) {
    }
}
