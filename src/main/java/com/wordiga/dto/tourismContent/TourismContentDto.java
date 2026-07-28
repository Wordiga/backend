package com.wordiga.dto.tourismContent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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

    @Schema(description = "주소", example = "충청남도 태안군 안면읍")
    private String addr1;

    @Schema(description = "대표 이미지 URL")
    private String firstImage;

    @Schema(description = "법정동 시군구 코드", example = "380")
    private String lDongSignguCd;

    @Schema(description = "경도")
    private BigDecimal mapx;

    @Schema(description = "위도")
    private BigDecimal mapy;

    @Schema(description = "콘텐츠 카테고리명", example = "관광지")
    private String categoryName;

    @Schema(description = "추천 점수")
    private BigDecimal recommendationScore;

    @Schema(description = "추천 이유")
    private List<String> recommendationReasons;
}
