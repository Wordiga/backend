package com.wordiga.api;

import com.wordiga.dto.wish.WishDeleteRequest;
import com.wordiga.dto.wish.WishFolderResponse;
import com.wordiga.dto.wish.WishRequest;
import com.wordiga.dto.wish.WishResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Wish", description = "위시리스트 관리 API")
public interface WishApi {

    @Operation(summary = "위시 등록",
            description = "콘텐츠 ID로 관광 정보를 조회하여 위시리스트에 등록합니다. " +
                    "충청남도 시군구에 따라 자동으로 분류하며, 이미 등록된 콘텐츠이면 기존 위시를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공 (또는 이미 존재)"),
            @ApiResponse(responseCode = "401", description = "인증 필요 - 로그인 필요")
    })
    ResponseEntity<WishResponse> addWish(Long memberId, WishRequest request);

    @Operation(summary = "위시 삭제",
            description = "등록된 위시를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ResponseEntity<Void> removeWish(Long memberId, WishDeleteRequest request);

    @Operation(summary = "위시 폴더 목록 조회",
            description = "충청남도 시군구 자동 폴더와 기본 위시리스트 폴더를 조회합니다. " +
                    "각 폴더의 위시 개수를 함께 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ResponseEntity<List<WishFolderResponse>> getWishFolders(Long memberId);

    @Operation(summary = "폴더별 위시 목록 조회",
            description = "특정 폴더(지역명 또는 '기본 위시리스트') 내의 위시 콘텐츠 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ResponseEntity<List<WishResponse>> getWishesByFolder(
            Long memberId,
            @Parameter(description = "폴더명 (시군구명 또는 '기본 위시리스트')") String folderName
    );
}
