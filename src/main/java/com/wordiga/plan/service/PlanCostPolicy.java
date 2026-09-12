package com.wordiga.plan.service;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.tourism.dto.detail.TourismDetailInfoDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class PlanCostPolicy {
    private static final Pattern AMOUNT = Pattern.compile("(?<!\\d)(\\d{1,3}(?:,\\d{3})+|\\d+)(?=\\s*원)");
    private static final Pattern COST_NAME = Pattern.compile("입장료|관람료|이용료|요금|비용");
    private static final Map<String, DefaultCost> DEFAULTS = Map.of(
            "12", new DefaultCost(10_000, Unit.PERSON, "관광지"),
            "14", new DefaultCost(10_000, Unit.PERSON, "문화시설"),
            "15", new DefaultCost(20_000, Unit.PERSON, "축제/행사"),
            "25", new DefaultCost(0, Unit.GROUP, "여행코스"),
            "28", new DefaultCost(30_000, Unit.PERSON, "레포츠"),
            "32", new DefaultCost(100_000, Unit.ROOM, "숙박"),
            "38", new DefaultCost(0, Unit.GROUP, "쇼핑"),
            "39", new DefaultCost(15_000, Unit.PERSON, "음식점"));

    private PlanCostPolicy() {
    }

    public static Estimate estimate(TourismContentDetailResponse detail, int participants) {
        String type = detail.getCommon().getContentTypeId();
        DefaultCost fallback = DEFAULTS.getOrDefault(type, new DefaultCost(0, Unit.GROUP, "기타"));
        List<Reference> references = references(detail, type);
        Long actual = references.stream().map(Reference::amount).filter(java.util.Objects::nonNull)
                .min(Long::compareTo).orElse(null);
        Unit unit = "32".equals(type) ? Unit.ROOM : fallback.unit();
        int quantity = unit == Unit.PERSON ? participants
                : unit == Unit.ROOM ? roomQuantity(detail, participants) : 1;
        long unitAmount = actual == null ? fallback.amount() : actual;
        return new Estimate(unitAmount, unit, quantity, unitAmount * quantity,
                actual == null, fallback.category(), references);
    }

    public static long defaultPerPersonAmount(String contentTypeId, int participants) {
        if (contentTypeId == null) return 0;
        DefaultCost cost = DEFAULTS.get(contentTypeId);
        if (cost == null) return 0;
        return switch (cost.unit()) {
            case PERSON -> cost.amount();
            case ROOM, GROUP -> cost.amount() / Math.max(1, participants);
        };
    }

    private static List<Reference> references(TourismContentDetailResponse detail, String type) {
        List<Reference> result = new ArrayList<>();
        if (detail.getIntro() != null && List.of("14", "15", "28").contains(type))
            add(result, "useFee", detail.getIntro().getUseFee());
        if (detail.getDetails() != null) for (TourismDetailInfoDto item : detail.getDetails()) {
            if ("32".equals(type)) {
                add(result, "roomOffSeasonWeekdayMinFee", item.getRoomOffSeasonWeekdayMinFee());
                add(result, "roomOffSeasonWeekendMinFee", item.getRoomOffSeasonWeekendMinFee());
                add(result, "roomPeakSeasonWeekdayMinFee", item.getRoomPeakSeasonWeekdayMinFee());
                add(result, "roomPeakSeasonWeekendMinFee", item.getRoomPeakSeasonWeekendMinFee());
            } else if (item.getInfoName() != null && COST_NAME.matcher(item.getInfoName()).find()) {
                add(result, item.getInfoName(), item.getInfoText());
            }
        }
        return result;
    }

    private static void add(List<Reference> result, String field, String raw) {
        if (raw == null || raw.isBlank()) return;
        Reference reference = new Reference(field, raw, amount(raw));
        if (!result.contains(reference)) result.add(reference);
    }

    private static Long amount(String raw) {
        if (raw.contains("무료")) return 0L;
        var matcher = AMOUNT.matcher(raw);
        return matcher.find() ? Long.parseLong(matcher.group(1).replace(",", "")) : null;
    }

    private static int roomQuantity(TourismContentDetailResponse detail, int participants) {
        int capacity = detail.getDetails() == null ? 0 : detail.getDetails().stream()
                .map(TourismDetailInfoDto::getRoomMaxCount).mapToInt(PlanCostPolicy::integer).max().orElse(0);
        return capacity > 0 ? (participants + capacity - 1) / capacity : 1;
    }

    private static int integer(String raw) {
        try {
            return raw == null ? 0 : Integer.parseInt(raw.replaceAll("\\D", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public enum Unit { PERSON, ROOM, GROUP }

    private record DefaultCost(long amount, Unit unit, String category) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Reference(String field, String rawValue, Long amount) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Estimate(long amount, Unit unit, int quantity, long calculatedAmount,
                           boolean fallbackApplied, String category, List<Reference> references) {
    }
}
