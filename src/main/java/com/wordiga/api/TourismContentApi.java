package com.wordiga.api;

import com.wordiga.dto.tourismContent.ListType;
import com.wordiga.dto.tourismContent.TourismContentListResponse;
import com.wordiga.dto.tourismContent.SigunguResponse;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Tourism Contents", description = "관광 콘텐츠 조회 API")
public interface TourismContentApi {

    @Operation(summary = "충청남도 시군구 조회")
    ResponseEntity<List<SigunguResponse>> getSigungu();

    @Operation(summary = "관광 콘텐츠 조회",
            description = "방문일과 검색 조건에 따라 충청남도 관광 콘텐츠를 조회합니다.")
    ResponseEntity<TourismContentListResponse> getTourismContentList(
            @Parameter(description = "추천 타입 (SEASONAL / POPULAR)") ListType type,
            LocalDate visitDate,
            String keyword,
            String contentTypeId,
            String lDongSignguCd,
            @Min(0) int page,
            @Min(1) @Max(50) int size
    );

    @Operation(summary = "관광 콘텐츠 통합 상세 조회",
            description = "공통정보, 타입별 소개·반복정보와 이미지를 통합하여 조회합니다.")
    ResponseEntity<TourismContentDetailResponse> getTourismContentDetail(
            @Parameter(description = "관광 콘텐츠 ID") String contentId,
            LocalDate visitDate,
            List<String> ageGroups,
            @Min(1) @Max(50) Integer participantCount
    );
}
