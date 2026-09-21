package com.wordiga.plan.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.AreaBasedItem;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.plan.Plan;
import com.wordiga.plan.dto.RainAlternativeResponse;
import com.wordiga.plan.repository.PlanRepository;
import com.wordiga.tourism.domain.TourismContentPolicy;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RainAlternativeService {
    private static final int SEARCH_LIMIT = 1000;
    private static final int MAX_DISTANCE_METERS = 20_000;
    private final PlanRepository planRepository;
    private final TourismApiClient tourismApiClient;
    private final TourismProperties tourismProperties;

    @Transactional(readOnly = true)
    public RainAlternativeResponse get(Long memberId, Long planId) {
        Plan plan = planRepository.findByIdAndMemberId(planId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."));
        List<TourismContentSnapshot> contents = plan.getPlanContents().stream()
                .map(content -> content.getContent()).distinct().toList();
        Set<String> scheduledIds = new HashSet<>();
        contents.forEach(content -> scheduledIds.add(content.getContentId()));
        Map<String, List<AreaBasedItem>> bySigungu = new HashMap<>();
        List<RainAlternativeResponse.Source> result = new ArrayList<>();
        for (TourismContentSnapshot source : contents) {
            if (!eligibleSource(source)) continue;
            String sigungu = source.getSigunguCode();
            if (sigungu == null || source.getMapx() == null || source.getMapy() == null) {
                result.add(new RainAlternativeResponse.Source(source.getContentId(),
                        outdoor(source), List.of()));
                continue;
            }
            List<AreaBasedItem> candidates = bySigungu.computeIfAbsent(sigungu, code ->
                    tourismApiClient.fetchAreaBasedContent(
                            tourismProperties.getRegion().getChungnamCode(), code, SEARCH_LIMIT));
            List<RainAlternativeResponse.Candidate> alternatives = candidates.stream()
                    .filter(item -> item.getContentid() != null && !scheduledIds.contains(item.getContentid()))
                    .filter(item -> sigungu.equals(item.getLDongSignguCd()))
                    .filter(item -> allowedAlternative(source, item))
                    .map(item -> new Ranked(item, distance(source.getMapx(), source.getMapy(),
                            item.getMapx(), item.getMapy()), categoryRank(source, item)))
                    .filter(item -> item.distanceMeters() <= MAX_DISTANCE_METERS)
                    .sorted(Comparator.comparingInt(Ranked::categoryRank)
                            .thenComparingInt(Ranked::distanceMeters))
                    .collect(LinkedHashMap<String, Ranked>::new,
                            (items, item) -> items.putIfAbsent(item.item().getContentid(), item),
                            LinkedHashMap::putAll)
                    .values().stream().limit(3)
                    .map(item -> new RainAlternativeResponse.Candidate(item.item().getContentid(),
                            item.item().getTitle(), item.item().getFirstimage(), item.distanceMeters()))
                    .toList();
            result.add(new RainAlternativeResponse.Source(source.getContentId(), outdoor(source), alternatives));
        }
        return new RainAlternativeResponse(planId, result);
    }

    private boolean eligibleSource(TourismContentSnapshot source) {
        if (source.getContentTypeId() == null
                || !Set.of("12", "14", "15", "28").contains(source.getContentTypeId())) return false;
        return !Boolean.FALSE.equals(outdoor(source));
    }

    private Boolean outdoor(TourismContentSnapshot source) {
        return TourismContentPolicy.isOutdoor(source.getContentTypeId(),
                source.getLclsSystem1Code(), source.getLclsSystem2Code());
    }

    private boolean allowedAlternative(TourismContentSnapshot source, AreaBasedItem candidate) {
        if (TourismContentPolicy.isCamping(candidate.getLclsSystm2())
                || !TourismContentPolicy.isIndoorAlternative(candidate.getLclsSystm2())) return false;
        return switch (source.getContentTypeId()) {
            case "28" -> "EX02".equals(candidate.getLclsSystm2())
                    || "VE07".equals(candidate.getLclsSystm2());
            case "15", "12", "14" -> Set.of("VE06", "VE07", "EX02").contains(candidate.getLclsSystm2());
            default -> false;
        };
    }

    private int categoryRank(TourismContentSnapshot source, AreaBasedItem candidate) {
        String middle = candidate.getLclsSystm2();
        if (Objects.equals(source.getLclsSystem2Code(), middle)) return 0;
        if ("28".equals(source.getContentTypeId())) return "EX02".equals(middle) ? 1 : 2;
        if ("15".equals(source.getContentTypeId())) return "VE06".equals(middle) ? 1 : 2;
        return "VE07".equals(middle) ? 1 : 2;
    }

    private int distance(BigDecimal x, BigDecimal y, String candidateX, String candidateY) {
        if (candidateX == null || candidateY == null) return Integer.MAX_VALUE;
        try {
            double lat1 = Math.toRadians(y.doubleValue());
            double lat2 = Math.toRadians(Double.parseDouble(candidateY));
            double dLat = lat2 - lat1;
            double dLon = Math.toRadians(Double.parseDouble(candidateX) - x.doubleValue());
            double a = Math.pow(Math.sin(dLat / 2), 2)
                    + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
            return (int) Math.round(12_742_000 * Math.asin(Math.min(1, Math.sqrt(a))));
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private record Ranked(AreaBasedItem item, int distanceMeters, int categoryRank) {
    }
}
