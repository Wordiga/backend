package com.wordiga.tourism.service;

import com.wordiga.plan.dto.PlanGenerateRequest;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.AreaBasedItem;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.tourism.domain.TourismContentType;
import com.wordiga.tourism.domain.TourismContentPolicy;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionalContentService {

    private static final int SEARCH_LIMIT = 1000;
    private static final List<CategoryTarget> CATEGORY_TARGETS = List.of(
            new CategoryTarget(Category.LODGING, 2),
            new CategoryTarget(Category.ATTRACTION, 5),
            new CategoryTarget(Category.RESTAURANT, 4),
            new CategoryTarget(Category.CAFE, 4)
    );

    private final TourismApiClient tourismApiClient;
    private final TourismProperties tourismProperties;
    private final TourismContentDetailService detailService;

    public List<TourismContentDetailResponse> find(PlanGenerateRequest request, List<TourismContentDetailResponse> savedDetails) {
        if (savedDetails == null || savedDetails.isEmpty()) {
            return List.of();
        }

        Set<String> savedContentIds = new HashSet<>(request.getSelectedContentIds());
        String regionCode = tourismProperties.getRegion().getChungnamCode();
        String sigunguCode = savedDetails.getFirst().getCommon().getLDongSignguCd();

        // 1. Saved 콘텐츠들의 중심점(Centroid) 계산
        Coordinate centroid = calculateCentroid(savedDetails);

        List<AreaBasedItem> localItems = tourismApiClient.fetchAreaBasedContent(regionCode, sigunguCode, SEARCH_LIMIT);
        List<AreaBasedItem> regionalItems = tourismApiClient.fetchAreaBasedContent(regionCode, null, SEARCH_LIMIT);
        List<TourismContentDetailResponse> details = new ArrayList<>();
        Set<String> includedIds = new HashSet<>(savedContentIds);
        for (CategoryTarget target : CATEGORY_TARGETS) {
            List<CandidateWithDistance> candidates = candidates(
                    localItems, regionalItems, target.category(), includedIds, centroid);
            int added = 0;
            for (CandidateWithDistance candidate : candidates) {
                if (added >= target.limit()) break;
                try {
                    TourismContentDetailResponse detail = detailService.getAiDetail(
                            candidate.item().getContentid(), YearMonth.parse(request.getVisitMonth()).atDay(1));
                    if (hasRequiredAiFields(detail)) {
                        details.add(detail);
                        includedIds.add(candidate.item().getContentid());
                        added++;
                    }
                } catch (RuntimeException exception) {
                    log.warn("[AI 일정] 지역 후보 상세 조회 실패: contentId={}",
                            candidate.item().getContentid(), exception);
                }
            }
        }
        return details;
    }

    private List<CandidateWithDistance> candidates(
            List<AreaBasedItem> localItems, List<AreaBasedItem> regionalItems, Category category,
            Set<String> excludedIds, Coordinate centroid) {
        List<CandidateWithDistance> local = sortedCandidates(
                localItems, category, excludedIds, centroid);
        Set<String> localIds = local.stream().map(candidate -> candidate.item().getContentid())
                .collect(java.util.stream.Collectors.toSet());
        localIds.addAll(excludedIds);
        List<CandidateWithDistance> regional = sortedCandidates(
                regionalItems, category, localIds, centroid);
        return java.util.stream.Stream.concat(local.stream(), regional.stream()).toList();
    }

    private List<CandidateWithDistance> sortedCandidates(
            List<AreaBasedItem> items, Category category, Set<String> excludedIds, Coordinate centroid) {
        return items.stream()
                .filter(item -> !TourismContentPolicy.isCamping(item.getLclsSystm2()))
                .filter(category::matches)
                .filter(item -> item.getContentid() != null && !excludedIds.contains(item.getContentid()))
                .collect(LinkedHashMap<String, AreaBasedItem>::new,
                        (unique, item) -> unique.putIfAbsent(item.getContentid(), item), LinkedHashMap::putAll)
                .values().stream()
                .map(item -> new CandidateWithDistance(item,
                        calculateDistance(centroid, item.getMapx(), item.getMapy())))
                .sorted(Comparator.comparingDouble(CandidateWithDistance::distance))
                .toList();
    }

    private boolean hasRequiredAiFields(TourismContentDetailResponse detail) {
        if (detail == null || detail.getCommon() == null) return false;
        var common = detail.getCommon();
        return common.getContentId() != null && common.getTitle() != null && common.getContentTypeId() != null
                && common.getMapx() != null && common.getMapy() != null && common.getLDongSignguCd() != null;
    }

    // ─── 거리 연산 Helper ───

    private Coordinate calculateCentroid(List<TourismContentDetailResponse> savedDetails) {
        double sumLat = 0.0;
        double sumLng = 0.0;
        int count = 0;

        for (var detail : savedDetails) {
            if (detail.getCommon() != null && detail.getCommon().getMapy() != null && detail.getCommon().getMapx() != null) {
                sumLat += detail.getCommon().getMapy().doubleValue();
                sumLng += detail.getCommon().getMapx().doubleValue();
                count++;
            }
        }

        return count == 0 ? new Coordinate(36.5, 126.8) : new Coordinate(sumLat / count, sumLng / count);
    }

    /**
     * 하버사인(Haversine) 공식을 이용한 두 좌표 간 거리(km) 계산
     */
    private double calculateDistance(Coordinate center, String mapxStr, String mapyStr) {
        if (mapxStr == null || mapyStr == null) return Double.MAX_VALUE;
        try {
            double lng = Double.parseDouble(mapxStr);
            double lat = Double.parseDouble(mapyStr);

            double earthRadius = 6371.0; // 지구 반지름 (km)
            double dLat = Math.toRadians(lat - center.lat());
            double dLng = Math.toRadians(lng - center.lng());

            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(center.lat())) * Math.cos(Math.toRadians(lat))
                    * Math.sin(dLng / 2) * Math.sin(dLng / 2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return earthRadius * c;
        } catch (NumberFormatException e) {
            return Double.MAX_VALUE;
        }
    }

    private record Coordinate(double lat, double lng) {
    }

    private record CandidateWithDistance(AreaBasedItem item, double distance) {
    }

    private record CategoryTarget(Category category, int limit) {
    }

    private enum Category {
        LODGING {
            boolean matches(AreaBasedItem item) {
                return TourismContentType.LODGING.getCode().equals(item.getContenttypeid());
            }
        },
        ATTRACTION {
            boolean matches(AreaBasedItem item) {
                return Set.of("12", "14", "15", "25", "28", "38").contains(item.getContenttypeid());
            }
        },
        RESTAURANT {
            boolean matches(AreaBasedItem item) {
                return TourismContentType.RESTAURANT.getCode().equals(item.getContenttypeid())
                        && !isCafe(item);
            }
        },
        CAFE {
            boolean matches(AreaBasedItem item) {
                return TourismContentType.RESTAURANT.getCode().equals(item.getContenttypeid())
                        && isCafe(item);
            }
        };

        abstract boolean matches(AreaBasedItem item);

        static boolean isCafe(AreaBasedItem item) {
            return item.getLclsSystm2() != null && item.getLclsSystm2().startsWith("FD05");
        }
    }
}
