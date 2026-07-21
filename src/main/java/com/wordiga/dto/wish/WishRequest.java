package com.wordiga.dto.wish;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "위시 등록 요청")
public class WishRequest {

    @NotBlank
    @Schema(description = "콘텐츠 ID", example = "126128")
    private String contentId;

    @Schema(description = "관광타입 ID", example = "12")
    private String contentTypeId;

    @NotBlank
    @Schema(description = "콘텐츠 제목", example = "을숙도 공원")
    private String title;

    @Schema(description = "대표 이미지 URL")
    private String firstimage;

    @Schema(description = "주소", example = "충남 천안시 동남구 ...")
    private String addr1;

    @Schema(description = "법정동 시군구 코드 (충남 16개 시군구)", example = "130")
    private String sigunguCode;
}