package com.wordiga.tourism.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "화면 선택용 코드와 표시명")
public record CodeNameDto(String code, String name) {
}
