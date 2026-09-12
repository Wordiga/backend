package com.wordiga.tourism.api;

import com.wordiga.tourism.dto.ListType;
import com.wordiga.tourism.dto.TourismContentListResponse;
import com.wordiga.tourism.dto.SigunguResponse;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Tourism Contents", description = "관광 콘텐츠 조회 API")
public interface TourismContentApi {

    @Operation(summary = "충청남도 시군구 조회")
    ResponseEntity<List<SigunguResponse>> getSigungu();

    @Operation(summary = "관광 콘텐츠 조회",
            description = "방문일과 검색 조건에 따라 충청남도 관광 콘텐츠를 조회합니다.")
    ResponseEntity<TourismContentListResponse> getTourismContentList(
            @Parameter(hidden = true) Long memberId,
            @Parameter(description = "목록 타입 (SEASONAL / POPULAR / PERSONALIZED / RELATED / FESTIVAL)") ListType type,
            @Parameter(description = "방문 월(YYYYMM)", example = "202609") String visitMonth,
            String keyword,
            String contentTypeId,
            String lDongSignguCd,
            String referenceContentId,
            Boolean capacitySatisfied,
            @Min(1) @Max(50) Integer participantCount,
            List<String> ageGroups,
            @Min(0) int page,
            @Min(1) @Max(50) int size
    );

    @Operation(summary = "관광 콘텐츠 통합 상세 조회",
            description = "공통정보, 타입별 소개·반복정보와 이미지를 통합하여 조회합니다.")
    ResponseEntity<TourismContentDetailResponse> getTourismContentDetail(
            @Parameter(description = "관광 콘텐츠 ID") String contentId,
            @Parameter(description = "방문 월(YYYYMM)", example = "202609") String visitMonth,
            List<String> ageGroups,
            @Min(1) @Max(3) Integer stayDays,
            @Min(1) @Max(50) Integer participantCount
    );
}
