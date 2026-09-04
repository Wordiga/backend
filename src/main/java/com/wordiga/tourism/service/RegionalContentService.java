package com.wordiga.tourism.service;

import com.wordiga.plan.dto.PlanGenerateRequest;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.AreaBasedItem;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.tourism.domain.TourismContentType;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionalContentService {

    private static final int CATEGORY_CANDIDATE_LIMIT = 6; // 카테고리별 상위 5~8개 추리기
    private static final int TOTAL_REGIONAL_LIMIT = 15;

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

        // 2. 카테고리별 후보군 수집 (관광지, 음식점, 숙박, 레포츠, 쇼핑 등)
        List<String> targetCategoryCodes = List.of(
                TourismContentType.TOURIST_ATTRACTION.getCode(), // 12
                TourismContentType.CULTURAL_FACILITY.getCode(),  // 14
                TourismContentType.LEPORTS.getCode(),            // 28
                TourismContentType.SHOPPING.getCode(),           // 38
                TourismContentType.RESTAURANT.getCode(),         // 39
                TourismContentType.LODGING.getCode()             // 32
        );

        Map<String, List<CandidateWithDistance>> categoryCandidatesMap = new LinkedHashMap<>();
        List<AreaBasedItem> areaItems = tourismApiClient.fetchAreaBasedContent(regionCode, sigunguCode, 100);

        for (String contentTypeId : targetCategoryCodes) {
            List<CandidateWithDistance> candidates = areaItems.stream()
                    .filter(item -> contentTypeId.equals(item.getContenttypeid()))
                    .filter(item -> !savedContentIds.contains(item.getContentid()))
                    .map(item -> new CandidateWithDistance(item, calculateDistance(centroid, item.getMapx(), item.getMapy())))
                    .sorted(Comparator.comparingDouble(CandidateWithDistance::distance)) // 중심점 기준 거리순 정렬
                    .limit(CATEGORY_CANDIDATE_LIMIT) // 카테고리별 5~8개로 추림
                    .toList();

            categoryCandidatesMap.put(contentTypeId, candidates);
        }

        // 3. 숙박 보장 조건 처리 (2일 이상 체류할 때 숙박 후보 최소 1개 이상 포함)
        boolean needLodging = request.getStayDays() >= 2;
        List<CandidateWithDistance> finalCandidates = new ArrayList<>();

        if (needLodging) {
            List<CandidateWithDistance> lodgings = categoryCandidatesMap.getOrDefault(TourismContentType.LODGING.getCode(), List.of());
            if (!lodgings.isEmpty()) {
                finalCandidates.add(lodgings.getFirst()); // 가장 가까운 숙박 1개 필수 포함
            }
        }

        // 4. 나머지 카테고리별 후보들 순차 수집 (최대 15개)
        for (List<CandidateWithDistance> list : categoryCandidatesMap.values()) {
            for (CandidateWithDistance candidate : list) {
                if (finalCandidates.size() >= TOTAL_REGIONAL_LIMIT) break;
                if (!finalCandidates.contains(candidate)) {
                    finalCandidates.add(candidate);
                }
            }
            if (finalCandidates.size() >= TOTAL_REGIONAL_LIMIT) break;
        }

        // 5. 최종 추린 candidate 들에 대해서만 상세(getAiDetail) 호출해 반환
        List<TourismContentDetailResponse> details = new ArrayList<>();
        for (CandidateWithDistance candidate : finalCandidates) {
            try {
                TourismContentDetailResponse detail = detailService.getAiDetail(
                        candidate.item().getContentid(), request.getStartDate(), false);
                if (hasRequiredAiFields(detail))
                    details.add(detail);
            } catch (RuntimeException exception) {
                log.warn("[AI 일정] 지역 후보 상세 조회 실패: contentId={}",
                        candidate.item().getContentid(), exception);
            }
        }
        return details;
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
}
