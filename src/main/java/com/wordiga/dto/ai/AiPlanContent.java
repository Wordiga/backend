package com.wordiga.dto.ai;

import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import com.wordiga.dto.tourismContent.detail.TourismDetailInfoDto;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
public class AiPlanContent {
    private static final Pattern AMOUNT = Pattern.compile("(?<!\\d)(\\d{1,3}(?:,\\d{3})+|\\d+)(?=\\s*원)");
    private static final Pattern COST_NAME = Pattern.compile("입장료|관람료|이용료|요금|비용");

    private String contentId;
    private Long estimatedCost;
    private List<String> costReferences;
    private TourismContentDetailResponse detail;

    public static AiPlanContent from(TourismContentDetailResponse detail) {
        List<String> references = new ArrayList<>();
        if (detail.getIntro() != null) {
            add(references, detail.getIntro().getUseFee());
        }
        if (detail.getDetails() != null) for (TourismDetailInfoDto item : detail.getDetails()) {
            add(references, item.getRoomOffSeasonWeekdayMinFee());
            add(references, item.getRoomOffSeasonWeekendMinFee());
            add(references, item.getRoomPeakSeasonWeekdayMinFee());
            add(references, item.getRoomPeakSeasonWeekendMinFee());
            if (item.getInfoName() != null && COST_NAME.matcher(item.getInfoName()).find()) add(references, item.getInfoText());
        }
        List<Long> amounts = references.stream().map(AiPlanContent::amount).filter(java.util.Objects::nonNull).toList();
        return builder()
                .contentId(detail.getCommon() == null ? null : detail.getCommon().getContentId())
                .estimatedCost(amounts.isEmpty() ? null : amounts.stream().min(Long::compareTo).orElseThrow())
                .costReferences(references)
                .detail(detail)
                .build();
    }

    private static void add(List<String> references, String value) {
        if (value != null && !value.isBlank() && !references.contains(value)) references.add(value);
    }

    private static Long amount(String value) {
        if (value.contains("무료")) return 0L;
        Matcher matcher = AMOUNT.matcher(value);
        return matcher.find() ? Long.parseLong(matcher.group(1).replace(",", "")) : null;
    }
}
