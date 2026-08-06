package com.wordiga.service;

import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.tourismContent.TourismContentDto;
import com.wordiga.dto.tourismContent.TourismContentListResponse;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.AreaBasedItem;
import com.wordiga.global.client.dto.AreaBasedResponse;
import com.wordiga.global.client.dto.RelatedTourismItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RelatedTourismContentService {
    private static final String CHUNGNAM = "44";
    private final TourismApiClient tourismApiClient;
    private final TourismContentDetailService detailService;
    private final TourismSatisfactionService satisfactionService;

    public TourismContentListResponse get(String referenceContentId, LocalDate visitDate,
                                          List<String> ageGroups, int page, int size) {
        ContentDetailDto reference = detailService.getCommonDetail(referenceContentId);
        List<RelatedTourismItem> related = tourismApiClient.fetchRelatedTourism(baseYm(), CHUNGNAM,
                CHUNGNAM + reference.getLDongSignguCd(), reference.getTitle(), 30);
        Map<String, TourismContentDto> candidates = new LinkedHashMap<>();
        var satisfactionContext = new TourismSatisfactionService.Context();
        for (RelatedTourismItem item : related.stream().sorted(Comparator.comparing(
                RelatedTourismItem::getRlteRank, Comparator.nullsLast(Integer::compareTo))).toList()) {
            AreaBasedItem match = exactMatch(item);
            if (match == null || referenceContentId.equals(match.getContentid())
                    || candidates.containsKey(match.getContentid())) continue;
            var satisfaction = satisfactionService.calculate(satisfactionContext, CHUNGNAM,
                    match.getLDongSignguCd(), match.getTitle(), visitDate, ageGroups);
            BigDecimal score = satisfaction == null ? BigDecimal.ZERO : satisfaction.getTotalScore();
            candidates.put(match.getContentid(), dto(match, score));
            if (candidates.size() == 30) break;
        }
        List<TourismContentDto> sorted = candidates.values().stream()
                .sorted(Comparator.comparing(TourismContentDto::getRecommendationScore).reversed()).toList();
        int from = Math.min(page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        return TourismContentListResponse.builder().items(sorted.subList(from, to)).page(page).size(size)
                .hasNext(to < sorted.size()).build();
    }

    private AreaBasedItem exactMatch(RelatedTourismItem item) {
        String signgu = item.getRlteSignguCd();
        if (item.getRlteTatsNm() == null || signgu == null || !signgu.startsWith(CHUNGNAM)) return null;
        AreaBasedResponse response = tourismApiClient.searchContent(item.getRlteTatsNm(), null, CHUNGNAM,
                signgu.substring(2), 1, 10);
        if (response == null || response.getResponse() == null || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody().getItems().getItem() == null) return null;
        return response.getResponse().getBody().getItems().getItem().stream()
                .filter(candidate -> item.getRlteTatsNm().equals(candidate.getTitle())).findFirst().orElse(null);
    }

    private TourismContentDto dto(AreaBasedItem item, BigDecimal score) {
        return TourismContentDto.builder().contentId(item.getContentid()).contentTypeId(item.getContenttypeid())
                .title(item.getTitle()).addr1(item.getAddr1()).firstImage(item.getFirstimage())
                .lDongSignguCd(item.getLDongSignguCd()).mapx(number(item.getMapx())).mapy(number(item.getMapy()))
                .recommendationScore(score).build();
    }

    private BigDecimal number(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String baseYm() {
        LocalDate now = LocalDate.now();
        return (now.getDayOfMonth() >= 16 ? now.minusMonths(1) : now.minusMonths(2))
                .format(DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
