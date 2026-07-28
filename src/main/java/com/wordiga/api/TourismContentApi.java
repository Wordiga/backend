package com.wordiga.api;

import com.wordiga.dto.tourismContent.ListType;
import com.wordiga.dto.tourismContent.TourismContentListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

@Tag(name = "Tourism Contents", description = "관광 콘텐츠 조회 API")
public interface TourismContentApi {

    @Operation(summary = "관광 콘텐츠 조회",
            description = "방문일과 검색 조건에 따라 충청남도 관광 콘텐츠를 조회합니다.")
    ResponseEntity<TourismContentListResponse> getTourismContentList(
            @Parameter(description = "추천 타입 (SEASONAL / POPULAR)") ListType type,
            LocalDate visitDate,
            String keyword,
            String contentTypeId,
            String lDongSignguCd,
            int page,
            int size
    );
}
