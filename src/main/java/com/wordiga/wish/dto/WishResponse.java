package com.wordiga.wish.dto;

import com.wordiga.wish.Wish;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "위시 아이템 응답")
public class WishResponse {

    @Schema(description = "위시 ID")
    private Long id;

    @Schema(description = "콘텐츠 ID")
    private String contentId;

    @Schema(description = "관광타입 ID")
    private String contentTypeId;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "대표 이미지")
    private String firstImage;

    @Schema(description = "주소")
    private String addr1;

    @Schema(description = "경도")
    private BigDecimal mapx;

    @Schema(description = "위도")
    private BigDecimal mapy;

    @Schema(description = "시군구 코드")
    private String lDongSignguCd;

    @Schema(description = "시군구명")
    private String sigunguName;

    @Schema(description = "폴더명")
    private String folderName;

    @Schema(description = "등록일시")
    private LocalDateTime createdAt;

    public static WishResponse from(Wish wish) {
        var content = wish.getContent();
        return WishResponse.builder()
                .id(wish.getId())
                .contentId(wish.getContentId())
                .contentTypeId(content.getContentTypeId())
                .title(content.getTitle())
                .firstImage(content.getFirstimage())
                .addr1(content.getAddr1())
                .mapx(content.getMapx())
                .mapy(content.getMapy())
                .lDongSignguCd(wish.getSigunguCode())
                .sigunguName(content.getSigunguName())
                .folderName(wish.getFolderName())
                .createdAt(wish.getCreatedAt())
                .build();
    }
}
