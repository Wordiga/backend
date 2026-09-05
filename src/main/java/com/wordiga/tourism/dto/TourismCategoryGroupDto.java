package com.wordiga.tourism.dto;

import java.util.List;

public record TourismCategoryGroupDto(CodeNameDto theme, List<CodeNameDto> categories) {
}
