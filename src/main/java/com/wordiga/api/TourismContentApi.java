package com.wordiga.api;

import com.wordiga.dto.tourismContent.ListType;
import com.wordiga.dto.tourismContent.TourismContentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Tourism Contents", description = "관광 콘텐츠 조회 API")
public interface TourismContentApi {

    @Operation(summary = "관광 콘텐츠 조회",
            description = "SEASONAL: 현재 시기 ±15일 행사/축제 추천 | POPULAR: 최근 업데이트된 인기 콘텐츠")
    ResponseEntity<List<TourismContentDto>> getTourismContentList(
            @Parameter(description = "추천 타입 (SEASONAL / POPULAR)") ListType type,
            String baseYm,
            @Parameter(description = "조회 개수 (기본 5, 최소 5)") int numOfRows
    );
}