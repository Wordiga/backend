package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.AreaBasedItem;
import com.wordiga.client.dto.AreaBasedResponse;
import com.wordiga.client.dto.RelatedTourismItem;
import com.wordiga.dto.plan.PlanGenerateRequest;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RegionalContentService {
    private static final String CHUNGNAM_CODE = "44";
    private static final int RELATED_PER_SAVED = 5;
    private static final int REGIONAL_LIMIT = 15;

    private final TourismApiClient tourismApiClient;
    private final TourismContentDetailService detailService;

    public List<TourismContentDetailResponse> find(
            PlanGenerateRequest request, List<TourismContentDetailResponse> saved) {
        Set<String> savedIds = new HashSet<>(request.getSelectedContentIds());
        Map<String, RelatedTourismItem> candidates = new LinkedHashMap<>();
        String baseYm = latestBaseYm();
        for (var detail : saved) {
            var common = detail.getCommon();
            if (common.getLDongSignguCd() == null) continue;
            tourismApiClient.fetchRelatedTourism(baseYm, CHUNGNAM_CODE,
                            CHUNGNAM_CODE + common.getLDongSignguCd(), common.getTitle(), RELATED_PER_SAVED)
                    .stream().sorted(Comparator.comparing(RelatedTourismItem::getRlteRank,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .filter(item -> CHUNGNAM_CODE.equals(item.getRlteRegnCd()))
                    .filter(item -> item.getRlteTatsNm() != null && !item.getRlteTatsNm().isBlank())
                    .forEach(item -> candidates.putIfAbsent(
                            item.getRlteSignguCd() + "\0" + item.getRlteTatsNm(), item));
        }

        List<TourismContentDetailResponse> regional = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (RelatedTourismItem candidate : candidates.values()) {
            if (regional.size() == REGIONAL_LIMIT) break;
            AreaBasedItem match = exactMatch(candidate);
            if (match == null || savedIds.contains(match.getContentid()) || !added.add(match.getContentid())) continue;
            regional.add(detailService.getAiDetail(match.getContentid(), request.getStartDate(), false));
        }
        return regional;
    }

    private AreaBasedItem exactMatch(RelatedTourismItem candidate) {
        String signgu = candidate.getRlteSignguCd();
        if (signgu == null || !signgu.startsWith(CHUNGNAM_CODE)) return null;
        return items(tourismApiClient.searchContent(candidate.getRlteTatsNm(), null, CHUNGNAM_CODE,
                        signgu.substring(CHUNGNAM_CODE.length()), 1, 10)).stream()
                .filter(item -> candidate.getRlteTatsNm().equals(item.getTitle()))
                .findFirst().orElse(null);
    }

    private List<AreaBasedItem> items(AreaBasedResponse response) {
        if (response == null || response.getResponse() == null || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody().getItems().getItem() == null) return List.of();
        return response.getResponse().getBody().getItems().getItem();
    }

    private String latestBaseYm() {
        LocalDate now = LocalDate.now();
        return (now.getDayOfMonth() >= 16 ? now.minusMonths(1) : now.minusMonths(2))
                .format(DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
