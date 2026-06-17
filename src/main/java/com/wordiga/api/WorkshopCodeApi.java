package com.wordiga.api;

import com.wordiga.dto.LclsSystmCodeDto;
import com.wordiga.dto.LdongCodeDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Workshop Codes", description = "법정동/분류체계 코드 조회 API")
public interface WorkshopCodeApi {

    @Operation(summary = "충남 시군구 법정동 코드 조회", description = "충남(lDongRegnCd=44) 하위 시군구 법정동 코드 목록 조회")
    ResponseEntity<List<LdongCodeDto>> getChungnamSignguCodes(
            @Parameter(description = "페이지 번호") int pageNo,
            @Parameter(description = "한 페이지 결과 수") int numOfRows
    );

    @Operation(summary = "분류체계 코드 조회", description = "대분류/중분류/소분류 코드를 계층적으로 조회")
    ResponseEntity<List<LclsSystmCodeDto>> getCategoryCodes(
            @Parameter(description = "페이지 번호") int pageNo,
            @Parameter(description = "한 페이지 결과 수") int numOfRows,
            @Parameter(description = "대분류 코드") String lclsSystm1,
            @Parameter(description = "중분류 코드") String lclsSystm2,
            @Parameter(description = "소분류 코드") String lclsSystm3,
            @Parameter(description = "목록조회 여부 (N: 코드조회, Y: 전체목록조회)") String lclsSystmListYn
    );
}