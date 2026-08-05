package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.AreaCulResDemItem;
import com.wordiga.client.dto.AreaTarExpDsItem;
import com.wordiga.client.dto.AreaTarSjrnDsItem;
import com.wordiga.client.dto.AreaTarSvcDemItem;
import com.wordiga.client.dto.KtoApiResponse;
import com.wordiga.client.dto.TatsCnctrRateItem;
import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.tourismContent.detail.SatisfactionDto;
import com.wordiga.dto.tourismContent.detail.ScoreComponentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
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
        String signguCode = ChungnamSigungu.DATA_LAB_CODES.get(localSignguCode);
        if (signguCode == null) return null;
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
        OptionalDouble comfort = comfort(context, areaCode, signguCode, title, visitDate);
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
                comfort, COMFORT_WEIGHT, "tatsCnctrRatedList");

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
                baseYm, areaCode, signguCode, "12"));
        List<AreaTarSvcDemItem> services = extractItems(tourismApiClient.fetchServiceDemand(
                baseYm, areaCode, signguCode, "11"));
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

        DoubleStream tourists = touristCodes.stream()
                .flatMap(code -> extractItems(tourismApiClient.fetchTouristDiversity(
                        baseYm, areaCode, signguCode, code)).stream())
                .mapToDouble(item -> parse(item.getTouDivIxVal()));
        DoubleStream expenditures = expenditureCodes.stream()
                .flatMap(code -> extractItems(tourismApiClient.fetchExpenditureDiversity(
                        baseYm, areaCode, signguCode, code)).stream())
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

    private OptionalDouble comfort(Context context,
            String areaCode, String signguCode, String title, LocalDate visitDate) {
        String targetDate = (visitDate == null ? LocalDate.now() : visitDate)
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        List<TatsCnctrRateItem> items = extractItems(tourismApiClient.fetchConcentrationRate(
                areaCode, signguCode, title));
        OptionalDouble exact = items.stream()
                .filter(item -> targetDate.equals(item.getBaseYmd()))
                .map(TatsCnctrRateItem::getCnctrRate)
                .map(this::numeric)
                .flatMapToDouble(OptionalDouble::stream)
                .map(value -> clamp(100 - value))
                .findFirst();
        if (exact.isPresent()) return exact;

        ComfortKey key = new ComfortKey(areaCode, signguCode, targetDate);
        return context.regionalComfort.computeIfAbsent(key, ignored -> {
            DoubleStream scores = extractItems(tourismApiClient.fetchConcentrationRate(
                    areaCode, signguCode, null)).stream()
                    .filter(item -> targetDate.equals(item.getBaseYmd()))
                    .map(TatsCnctrRateItem::getCnctrRate)
                    .map(this::numeric)
                    .flatMapToDouble(OptionalDouble::stream)
                    .map(value -> clamp(100 - value));
            return scores.average();
        });
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
        private final Map<ComfortKey, OptionalDouble> regionalComfort = new HashMap<>();
    }

    private record RegionKey(String baseYm, String areaCode, String signguCode) {
    }

    private record AgeKey(RegionKey region, List<String> ageGroups) {
    }

    private record ComfortKey(String areaCode, String signguCode, String targetDate) {
    }
}
