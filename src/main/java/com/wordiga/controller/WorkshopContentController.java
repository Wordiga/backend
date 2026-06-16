package com.wordiga.controller;

import com.wordiga.api.WorkshopContentApi;
import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.ContentListDto;
import com.wordiga.service.WorkshopContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workshops/contents")
@RequiredArgsConstructor
public class WorkshopContentController implements WorkshopContentApi {

    private final WorkshopContentService workshopContentService;

    @GetMapping
    public ResponseEntity<List<ContentListDto>> getWorkshopContents(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "numOfRows", defaultValue = "10") int numOfRows,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "lDongSignguCd", required = false) String lDongSignguCd,
            @RequestParam(value = "lclsSystm1", required = false) String lclsSystm1,
            @RequestParam(value = "lclsSystm2", required = false) String lclsSystm2,
            @RequestParam(value = "lclsSystm3", required = false) String lclsSystm3) {

        List<ContentListDto> contents = workshopContentService.fetchContents(
                pageNo, numOfRows, keyword, lDongSignguCd, lclsSystm1, lclsSystm2, lclsSystm3
        );
        return ResponseEntity.ok(contents);
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDetailDto> getWorkshopContentDetail(
            @PathVariable String contentId) {

        ContentDetailDto detail = workshopContentService.fetchContentDetail(contentId);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }
}