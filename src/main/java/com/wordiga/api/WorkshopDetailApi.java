package com.wordiga.api;

import com.wordiga.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Workshop Detail", description = "관광 콘텐츠 상세/행사/숙박 조회 API")
public interface WorkshopDetailApi {

    @Operation(summary = "공통정보 조회", description = "콘텐츠 ID로 기본정보, 주소, 좌표, 개요 등을 조회")
    ResponseEntity<ContentDetailDto> getCommonDetail(
            @Parameter(description = "콘텐츠 ID") String contentId
    );

    @Operation(summary = "소개정보 조회", description = "콘텐츠 ID + 관광타입 ID로 소개정보(휴무일, 이용시간 등) 조회")
    ResponseEntity<DetailIntroDto> getIntroDetail(
            @Parameter(description = "콘텐츠 ID") String contentId,
            @Parameter(description = "관광타입 ID") String contentTypeId
    );

    @Operation(summary = "반복정보 조회", description = "콘텐츠 ID + 관광타입 ID로 반복 상세정보 조회 (객실정보, 코스정보 등)")
    ResponseEntity<List<DetailInfoDto>> getRepeatInfo(
            @Parameter(description = "콘텐츠 ID") String contentId,
            @Parameter(description = "관광타입 ID") String contentTypeId,
            @Parameter(description = "페이지 번호") int pageNo,
            @Parameter(description = "한 페이지 결과 수") int numOfRows
    );

    @Operation(summary = "이미지정보 조회", description = "콘텐츠 ID로 이미지 URL 목록 조회")
    ResponseEntity<List<DetailImageDto>> getImages(
            @Parameter(description = "콘텐츠 ID") String contentId,
            @Parameter(description = "이미지 조회 여부 (Y=콘텐츠이미지, N=음식메뉴이미지)") String imageYN,
            @Parameter(description = "페이지 번호") int pageNo,
            @Parameter(description = "한 페이지 결과 수") int numOfRows
    );

    @Operation(summary = "행사정보 조회", description = "충남 지역 행사/공연/축제 정보를 날짜 기반으로 조회")
    ResponseEntity<List<FestivalListDto>> getFestivals(
            @Parameter(description = "페이지 번호") int pageNo,
            @Parameter(description = "한 페이지 결과 수") int numOfRows,
            @Parameter(description = "행사 시작일 (YYYYMMDD, 필수)") String eventStartDate,
            @Parameter(description = "행사 종료일 (YYYYMMDD)") String eventEndDate,
            @Parameter(description = "법정동 시군구 코드") String lDongSignguCd,
            @Parameter(description = "분류체계 대분류") String lclsSystm1,
            @Parameter(description = "분류체계 중분류") String lclsSystm2,
            @Parameter(description = "분류체계 소분류") String lclsSystm3
    );

    @Operation(summary = "숙박정보 조회", description = "충남 지역 숙박 정보 목록 조회")
    ResponseEntity<List<StayListDto>> getStays(
            @Parameter(description = "페이지 번호") int pageNo,
            @Parameter(description = "한 페이지 결과 수") int numOfRows,
            @Parameter(description = "법정동 시군구 코드") String lDongSignguCd,
            @Parameter(description = "분류체계 대분류") String lclsSystm1,
            @Parameter(description = "분류체계 중분류") String lclsSystm2,
            @Parameter(description = "분류체계 소분류") String lclsSystm3
    );
}