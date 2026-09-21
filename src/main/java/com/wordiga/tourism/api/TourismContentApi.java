package com.wordiga.tourism.api;

import com.wordiga.global.security.CurrentMemberId;
import com.wordiga.tourism.dto.ListType;
import com.wordiga.tourism.dto.SigunguResponse;
import com.wordiga.tourism.dto.TourismContentListResponse;
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
            @Parameter(description = "방문 월(YYYY-MM)", example = "2026-09") String visitMonth,
            String keyword,
            String contentTypeId,
            @Parameter(description = "화면 대분류 테마 코드") String theme,
            @Parameter(description = "화면 세부 카테고리 코드 목록") List<String> category,
            String lDongSignguCd,
            String referenceContentId,
            Boolean capacitySatisfied,
            @Min(1) @Max(50) Integer participantCount,
            List<String> ageGroups,
            @Parameter(description = "체류 일수. 상세 조회와 동일한 만족도 계산에 사용") @Min(1) @Max(3) Integer stayDays,
            @Min(0) int page,
            @Min(1) @Max(50) int size
    );

    @Operation(summary = "일정 편집 장소 목록 조회",
            description = "축제와 숙소를 제외한 충청남도 장소를 조회합니다.")
    ResponseEntity<TourismContentListResponse> getPlaces(
            @Parameter(hidden = true) Long memberId,
            ListType type,
            String keyword,
            String lDongSignguCd,
            @Min(0) int page,
            @Min(1) @Max(50) int size
    );

    @Operation(summary = "일정 편집 숙소 목록 조회",
            description = "충청남도 숙소만 조회합니다.")
    ResponseEntity<TourismContentListResponse> getLodgings(
            @Parameter(hidden = true) Long memberId,
            String keyword,
            String lDongSignguCd,
            @Min(0) int page,
            @Min(1) @Max(50) int size
    );

    @Operation(summary = "축제·행사 목록 조회",
            description = "방문 월에 관람 가능한 충청남도 축제·행사를 조회합니다.")
    ResponseEntity<TourismContentListResponse> getFestivals(
            @Parameter(hidden = true) Long memberId,
            @Parameter(description = "방문 월(YYYY-MM)", example = "2026-09") String visitMonth,
            String lDongSignguCd,
            @Min(0) int page,
            @Min(1) @Max(50) int size
    );

    @Operation(summary = "관광 콘텐츠 상세 조회",
            description = "공통/소개/반복 정보, 계절 이미지, 만족도 분석, 수용 인원 적합도를 통합 반환합니다.")
    ResponseEntity<TourismContentDetailResponse> getTourismContentDetail(
            @CurrentMemberId Long memberId,
            @Parameter(description = "콘텐츠 ID") String contentId,
            @Parameter(description = "방문 예정월 (YYYY-MM)", example = "2026-09") String visitMonth,
            List<String> ageGroups,
            @Min(1) @Max(3) Integer stayDays,
            @Min(1) @Max(50) Integer participantCount
    );
}
