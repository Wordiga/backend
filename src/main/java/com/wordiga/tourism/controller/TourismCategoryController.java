package com.wordiga.tourism.controller;

import com.wordiga.tourism.dto.TourismCategoryGroupDto;
import com.wordiga.tourism.service.TourismContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tourism/categories")
@RequiredArgsConstructor
@Tag(name = "Tourism Contents", description = "관광 콘텐츠 조회 API")
public class TourismCategoryController {
    private final TourismContentService tourismContentService;

    @GetMapping
    @Operation(summary = "관광 테마와 세부 카테고리 조회")
    public ResponseEntity<List<TourismCategoryGroupDto>> getCategories() {
        return ResponseEntity.ok(tourismContentService.getCategories());
    }
}
