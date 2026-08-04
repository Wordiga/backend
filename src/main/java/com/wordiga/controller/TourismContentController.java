package com.wordiga.controller;

import com.wordiga.api.TourismContentApi;
import com.wordiga.dto.tourismContent.ListType;
import com.wordiga.dto.tourismContent.TourismContentListResponse;
import com.wordiga.dto.tourismContent.SigunguResponse;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import com.wordiga.service.TourismContentDetailService;
import com.wordiga.service.TourismContentService;
import com.wordiga.service.ChungnamSigungu;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/v1/tourism/contents")
@RequiredArgsConstructor
@Validated
public class TourismContentController implements TourismContentApi {

    private final TourismContentService tourismContentService;
    private final TourismContentDetailService tourismContentDetailService;

    @GetMapping("/sigungu")
    public ResponseEntity<List<SigunguResponse>> getSigungu() {
        return ResponseEntity.ok(ChungnamSigungu.ALL);
    }

    @GetMapping
    public ResponseEntity<TourismContentListResponse> getTourismContentList(
            @RequestParam(value = "type", defaultValue = "POPULAR") ListType type,
            @RequestParam(required = false) LocalDate visitDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contentTypeId,
            @RequestParam(required = false) String lDongSignguCd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(tourismContentService.getContentList(
                type, visitDate, keyword, contentTypeId, lDongSignguCd, page, size));
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<TourismContentDetailResponse> getTourismContentDetail(
            @PathVariable String contentId,
            @RequestParam(required = false) LocalDate visitDate,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) Integer participantCount) {
        return ResponseEntity.ok(tourismContentDetailService.getDetail(contentId, visitDate, ageGroups));
    }
}
