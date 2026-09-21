package com.wordiga.tourism.controller;

import com.wordiga.global.security.CurrentMemberId;
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
import java.time.YearMonth;
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
            @RequestParam(required = false) String visitMonth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contentTypeId,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) String lDongSignguCd,
            @RequestParam(required = false) String referenceContentId,
            @RequestParam(required = false) Boolean capacitySatisfied,
            @RequestParam(defaultValue = "10") Integer participantCount,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(tourismContentService.getContentList(
                memberId, type, visitDate(visitMonth), keyword, contentTypeId, theme, category, lDongSignguCd,
                referenceContentId, capacitySatisfied, participantCount, ageGroups, page, size));
    }

    @GetMapping("/places")
    public ResponseEntity<TourismContentListResponse> getPlaces(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(value = "type", defaultValue = "POPULAR") ListType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String lDongSignguCd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tourismContentService.getPlaces(
                memberId, type, keyword, lDongSignguCd, page, size));
    }

    @GetMapping("/lodgings")
    public ResponseEntity<TourismContentListResponse> getLodgings(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String lDongSignguCd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tourismContentService.getLodgings(
                memberId, keyword, lDongSignguCd, page, size));
    }

    @GetMapping("/festivals")
    public ResponseEntity<TourismContentListResponse> getFestivals(
            @AuthenticationPrincipal Long memberId,
            @RequestParam String visitMonth,
            @RequestParam(required = false) String lDongSignguCd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDate visitDate = visitDate(visitMonth);
        return ResponseEntity.ok(tourismContentService.getFestivals(
                memberId, visitDate, lDongSignguCd, page, size));
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<TourismContentDetailResponse> getTourismContentDetail(
            @CurrentMemberId Long memberId,
            @PathVariable String contentId,
            @RequestParam(required = false) String visitMonth,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) Integer stayDays,
            @RequestParam(defaultValue = "10") Integer participantCount) {
        return ResponseEntity.ok(tourismContentDetailService.getDetail(
                memberId, contentId, visitDate(visitMonth), ageGroups, stayDays == null ? null : stayDays - 1, participantCount));
    }

    private LocalDate visitDate(String visitMonth) {
        if (visitMonth == null || visitMonth.isBlank()) return null;
        try {
            return YearMonth.parse(visitMonth).atDay(1);
        } catch (RuntimeException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "방문 월은 YYYY-MM 형식이어야 합니다.");
        }
    }
}
