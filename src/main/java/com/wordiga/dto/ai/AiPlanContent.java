package com.wordiga.dto.ai;

import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import com.wordiga.dto.tourismContent.detail.TourismDetailInfoDto;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
public class AiPlanContent {
    private static final Pattern AMOUNT = Pattern.compile("(?<!\\d)(\\d{1,3}(?:,\\d{3})+|\\d+)(?=\\s*원)");
    private static final Pattern COST_NAME = Pattern.compile("입장료|관람료|이용료|요금|비용");
    private static final Map<String, DefaultCost> DEFAULT_COSTS = Map.of(
            "12", new DefaultCost(10_000, Unit.PERSON), "14", new DefaultCost(10_000, Unit.PERSON),
            "15", new DefaultCost(20_000, Unit.PERSON), "25", new DefaultCost(0, Unit.GROUP),
            "28", new DefaultCost(30_000, Unit.PERSON), "32", new DefaultCost(100_000, Unit.ROOM),
            "38", new DefaultCost(0, Unit.GROUP), "39", new DefaultCost(15_000, Unit.PERSON));

    private String contentId;
    private Cost cost;
    private TourismContentDetailResponse detail;

    public static AiPlanContent from(TourismContentDetailResponse detail, int participantCount) {
        String type = detail.getCommon() == null ? null : detail.getCommon().getContentTypeId();
        DefaultCost fallback = type == null ? null : DEFAULT_COSTS.get(type);
        List<Reference> references = references(detail, type);
        List<Long> amounts = references.stream().map(Reference::amount).filter(java.util.Objects::nonNull).toList();
        Unit unit = "32".equals(type) ? Unit.ROOM : fallback == null ? Unit.GROUP : fallback.unit();
        int quantity = unit == Unit.PERSON ? participantCount : unit == Unit.ROOM ? roomQuantity(detail, participantCount) : 1;
        Long actual = amounts.isEmpty() ? null : amounts.stream().min(Long::compareTo).orElseThrow();
        Long fallbackAmount = actual == null && fallback != null ? fallback.amount() : null;
        return builder()
                .contentId(detail.getCommon() == null ? null : detail.getCommon().getContentId())
                .cost(Cost.builder().amount(actual).unit(unit).quantity(quantity)
                        .calculatedAmount(multiply(actual, quantity)).fallbackAmount(fallbackAmount)
                        .fallbackCalculatedAmount(multiply(fallbackAmount, quantity))
                        .fallbackApplied(actual == null && fallback != null).references(references).build())
                .detail(detail)
                .build();
    }

    private static List<Reference> references(TourismContentDetailResponse detail, String type) {
        List<Reference> result = new ArrayList<>();
        if (detail.getIntro() != null && type != null && List.of("14", "15", "28").contains(type))
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

    private static int roomQuantity(TourismContentDetailResponse detail, int participants) {
        int max = detail.getDetails() == null ? 0 : detail.getDetails().stream()
                .map(TourismDetailInfoDto::getRoomMaxCount).mapToInt(AiPlanContent::integer).max().orElse(0);
        return max > 0 ? (participants + max - 1) / max : 1;
    }

    private static void add(List<Reference> references, String field, String raw) {
        if (raw != null && !raw.isBlank() && references.stream().noneMatch(r -> r.field().equals(field) && r.rawValue().equals(raw)))
            references.add(new Reference(field, raw, amount(raw)));
    }

    private static Long amount(String value) {
        if (value.contains("무료")) return 0L;
        Matcher matcher = AMOUNT.matcher(value);
        return matcher.find() ? Long.parseLong(matcher.group(1).replace(",", "")) : null;
    }

    private static int integer(String value) {
        try { return value == null ? 0 : Integer.parseInt(value.replaceAll("\\D", "")); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static Long multiply(Long amount, int quantity) { return amount == null ? null : amount * quantity; }

    public enum Unit { PERSON, ROOM, GROUP }
    private record DefaultCost(long amount, Unit unit) { }
    public record Reference(String field, String rawValue, Long amount) { }

    @Data
    @Builder
    public static class Cost {
        private Long amount;
        private Unit unit;
        private int quantity;
        private Long calculatedAmount;
        private Long fallbackAmount;
        private Long fallbackCalculatedAmount;
        private boolean fallbackApplied;
        private List<Reference> references;
    }
}
