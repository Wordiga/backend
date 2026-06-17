package com.wordiga.controller;

import com.wordiga.api.WorkshopDetailApi;
import com.wordiga.dto.*;
import com.wordiga.service.WorkshopDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workshops/details")
@RequiredArgsConstructor
public class WorkshopDetailController implements WorkshopDetailApi {

    private final WorkshopDetailService workshopDetailService;

    @GetMapping("/common/{contentId}")
    public ResponseEntity<ContentDetailDto> getCommonDetail(
            @PathVariable String contentId) {

        ContentDetailDto detail = workshopDetailService.fetchCommonDetail(contentId);
        if (detail == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/intro/{contentId}")
    public ResponseEntity<DetailIntroDto> getIntroDetail(
            @PathVariable String contentId,
            @RequestParam("contentTypeId") String contentTypeId) {

        DetailIntroDto intro = workshopDetailService.fetchIntroDetail(contentId, contentTypeId);
        if (intro == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(intro);
    }

    @GetMapping("/info/{contentId}")
    public ResponseEntity<List<DetailInfoDto>> getRepeatInfo(
            @PathVariable String contentId,
            @RequestParam("contentTypeId") String contentTypeId,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "numOfRows", defaultValue = "10") int numOfRows) {

        List<DetailInfoDto> list = workshopDetailService.fetchRepeatInfo(contentId, contentTypeId, pageNo, numOfRows);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/images/{contentId}")
    public ResponseEntity<List<DetailImageDto>> getImages(
            @PathVariable String contentId,
            @RequestParam(value = "imageYN", defaultValue = "Y") String imageYN,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "numOfRows", defaultValue = "10") int numOfRows) {

        List<DetailImageDto> list = workshopDetailService.fetchImages(contentId, imageYN, pageNo, numOfRows);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/festivals")
    public ResponseEntity<List<FestivalListDto>> getFestivals(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "numOfRows", defaultValue = "10") int numOfRows,
            @RequestParam("eventStartDate") String eventStartDate,
            @RequestParam(value = "eventEndDate", required = false) String eventEndDate,
            @RequestParam(value = "lDongSignguCd", required = false) String lDongSignguCd,
            @RequestParam(value = "lclsSystm1", required = false) String lclsSystm1,
            @RequestParam(value = "lclsSystm2", required = false) String lclsSystm2,
            @RequestParam(value = "lclsSystm3", required = false) String lclsSystm3) {

        List<FestivalListDto> list = workshopDetailService.fetchFestivals(
                pageNo, numOfRows, eventStartDate, eventEndDate,
                lDongSignguCd, lclsSystm1, lclsSystm2, lclsSystm3
        );
        return ResponseEntity.ok(list);
    }

    @GetMapping("/stays")
    public ResponseEntity<List<StayListDto>> getStays(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "numOfRows", defaultValue = "10") int numOfRows,
            @RequestParam(value = "lDongSignguCd", required = false) String lDongSignguCd,
            @RequestParam(value = "lclsSystm1", required = false) String lclsSystm1,
            @RequestParam(value = "lclsSystm2", required = false) String lclsSystm2,
            @RequestParam(value = "lclsSystm3", required = false) String lclsSystm3) {

        List<StayListDto> list = workshopDetailService.fetchStays(
                pageNo, numOfRows, lDongSignguCd, lclsSystm1, lclsSystm2, lclsSystm3
        );
        return ResponseEntity.ok(list);
    }
}