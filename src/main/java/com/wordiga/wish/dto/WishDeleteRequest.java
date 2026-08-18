package com.wordiga.wish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "위시 삭제 요청")
public class WishDeleteRequest {

    @NotBlank
    @Schema(description = "삭제할 콘텐츠 ID", example = "126128")
    private String contentId;
}