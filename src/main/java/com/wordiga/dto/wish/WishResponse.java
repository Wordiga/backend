package com.wordiga.dto.wish;

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
        return WishResponse.builder()
                .id(wish.getId())
                .contentId(wish.getContentId())
                .contentTypeId(wish.getContentTypeId())
                .title(wish.getTitle())
                .firstImage(wish.getFirstimage())
                .addr1(wish.getAddr1())
                .mapx(wish.getMapx())
                .mapy(wish.getMapy())
                .lDongSignguCd(wish.getSigunguCode())
                .sigunguName(wish.getSigunguName())
                .folderName(wish.getFolderName())
                .createdAt(wish.getCreatedAt())
                .build();
    }
}
