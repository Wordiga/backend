package com.wordiga.member.api;

import com.wordiga.member.dto.MemberProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Member", description = "회원 프로필 및 탈퇴 API")
public interface MemberApi {
    @Operation(summary = "내 프로필 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "회원 없음")
    })
    ResponseEntity<MemberProfileResponse> getProfile(@Parameter(hidden = true) Long memberId);

    @Operation(summary = "회원탈퇴", description = "소셜 연결과 Wordiga 저장 데이터를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "회원 없음"),
            @ApiResponse(responseCode = "503", description = "소셜 연결 또는 파일 삭제 실패")
    })
    ResponseEntity<Void> withdraw(@Parameter(hidden = true) Long memberId);
}
