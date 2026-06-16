package com.wordiga.api;

import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.ContentListDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Workshop Contents", description = "충남 관광 콘텐츠 조회 API")
public interface WorkshopContentApi {

    @Operation(summary = "관광 콘텐츠 목록 조회", description = "keyword 입력 시 키워드 검색(searchKeyword2), 미입력 시 지역기반 목록 조회(areaBasedList2)")
    ResponseEntity<List<ContentListDto>> getWorkshopContents(
            @Parameter(description = "페이지 번호") int pageNo,
            @Parameter(description = "한 페이지 결과 수") int numOfRows,
            @Parameter(description = "검색 키워드 (없으면 전체 목록)") String keyword,
            @Parameter(description = "법정동 시군구 코드") String lDongSignguCd,
            @Parameter(description = "분류체계 대분류") String lclsSystm1,
            @Parameter(description = "분류체계 중분류") String lclsSystm2,
            @Parameter(description = "분류체계 소분류") String lclsSystm3
    );

    @Operation(summary = "관광 콘텐츠 상세 조회", description = "contentId로 공통정보(주소, 좌표, 개요 등) 조회")
    ResponseEntity<ContentDetailDto> getWorkshopContentDetail(
            @Parameter(description = "콘텐츠 ID") String contentId
    );
}