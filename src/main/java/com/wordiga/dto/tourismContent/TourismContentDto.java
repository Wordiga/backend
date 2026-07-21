package com.wordiga.dto.tourismContent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "관광 콘텐츠 응답")
public class TourismContentDto {

    @Schema(description = "콘텐츠 ID", example = "126128")
    private String contentId;

    @Schema(description = "관광타입 ID", example = "12")
    private String contentTypeId;

    @Schema(description = "제목", example = "안면도 꽃지 해수욕장")
    private String title;

    @Schema(description = "주소 (시군구까지)", example = "충남 태안군")
    private String location;

    @Schema(description = "대표 이미지 URL")
    private String firstimage;

    @Schema(description = "예상 소요 시간 (분)", example = "90")
    private Integer estimatedDurationMin;

    @Schema(description = "콘텐츠 카테고리명", example = "관광지")
    private String categoryName;
}