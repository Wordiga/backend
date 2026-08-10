package com.wordiga.tourism.controller;

import com.wordiga.tourism.api.TourismContentApi;
import com.wordiga.tourism.dto.ListType;
import com.wordiga.tourism.dto.SigunguResponse;
import com.wordiga.tourism.dto.TourismContentListResponse;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.tourism.service.TourismContentDetailService;
import com.wordiga.tourism.service.TourismContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.ok(tourismContentService.getSigunguList());
    }

    @GetMapping
    public ResponseEntity<TourismContentListResponse> getTourismContentList(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(value = "type", defaultValue = "POPULAR") ListType type,
            @RequestParam(required = false) LocalDate visitDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contentTypeId,
            @RequestParam(required = false) String lDongSignguCd,
            @RequestParam(required = false) String referenceContentId,
            @RequestParam(required = false) Boolean capacitySatisfied,
            @RequestParam(required = false) Integer participantCount,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(tourismContentService.getContentList(
                memberId, type, visitDate, keyword, contentTypeId, lDongSignguCd, referenceContentId,
                capacitySatisfied, participantCount, ageGroups, page, size));
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<TourismContentDetailResponse> getTourismContentDetail(
            @PathVariable String contentId,
            @RequestParam(required = false) LocalDate visitDate,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) Integer participantCount) {
        return ResponseEntity.ok(tourismContentDetailService.getDetail(contentId, visitDate, ageGroups, participantCount));
    }
}
