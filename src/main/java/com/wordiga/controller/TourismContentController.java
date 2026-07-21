package com.wordiga.controller;

import com.wordiga.api.TourismContentApi;
import com.wordiga.dto.tourismContent.ListType;
import com.wordiga.dto.tourismContent.TourismContentDto;
import com.wordiga.service.TourismContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tourism/contents")
@RequiredArgsConstructor
public class TourismContentController implements TourismContentApi {

    private final TourismContentService tourismContentService;

    @GetMapping
    public ResponseEntity<List<TourismContentDto>> getTourismContentList(
            @RequestParam(value = "type", defaultValue = "POPULAR") ListType type,
            @RequestParam(required = false) String baseYm,
            @RequestParam(value = "numOfRows", defaultValue = "5") int numOfRows) {

        List<TourismContentDto> recommendations = tourismContentService.fetchListType(type, baseYm, numOfRows);
        return ResponseEntity.ok(recommendations);
    }
}