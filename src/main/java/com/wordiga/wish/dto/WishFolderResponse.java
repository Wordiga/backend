package com.wordiga.wish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "위시 폴더 요약 응답")
public class WishFolderResponse {

    @Schema(description = "폴더명 (시군구명 또는 '기본 위시리스트')", example = "천안시")
    private String folderName;

    @Schema(description = "시군구 코드", example = "130")
    private String lDongSignguCd;

    @Schema(description = "폴더 내 위시 개수", example = "5")
    private Long count;

    @Schema(description = "폴더에서 가장 최근에 저장한 위시 이미지")
    private String thumbnailUrl;
}
