package com.wordiga.tourism.dto;

import com.wordiga.plan.dto.ContentCostDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

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

    @Schema(description = "화면 대분류 테마")
    private CodeNameDto theme;

    @Schema(description = "화면 세부 카테고리")
    private CodeNameDto category;

    @Schema(description = "추천 점수")
    private BigDecimal recommendationScore;

    @Schema(description = "상세 만족도 최종 점수와 동일한 값")
    private BigDecimal satisfactionScore;

    @Schema(description = "관광타입 기본 정책 기준 1인 예상 비용")
    private Long estimatedCost;

    @Schema(description = "콘텐츠 유형별 기본 예상 비용")
    private ContentCostDto cost;

    @Schema(description = "현재 로그인 회원의 위시 등록 여부")
    @JsonProperty("isWished")
    @Getter(onMethod_ = @JsonProperty("isWished"))
    private boolean isWished;

}
