package com.wordiga.controller;

import com.wordiga.api.WorkshopCodeApi;
import com.wordiga.dto.LclsSystmCodeDto;
import com.wordiga.dto.LdongCodeDto;
import com.wordiga.service.WorkshopCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workshops/codes")
@RequiredArgsConstructor
public class WorkshopCodeController implements WorkshopCodeApi {

    private final WorkshopCodeService workshopCodeService;

    @GetMapping("/regions/chungnam")
    public ResponseEntity<List<LdongCodeDto>> getChungnamSignguCodes(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "numOfRows", defaultValue = "30") int numOfRows) {

        List<LdongCodeDto> codes = workshopCodeService.fetchChungnamSignguCodes(pageNo, numOfRows);
        return ResponseEntity.ok(codes);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<LclsSystmCodeDto>> getCategoryCodes(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "numOfRows", defaultValue = "50") int numOfRows,
            @RequestParam(value = "lclsSystm1", required = false) String lclsSystm1,
            @RequestParam(value = "lclsSystm2", required = false) String lclsSystm2,
            @RequestParam(value = "lclsSystm3", required = false) String lclsSystm3,
            @RequestParam(value = "lclsSystmListYn", defaultValue = "N") String lclsSystmListYn) {

        List<LclsSystmCodeDto> codes = workshopCodeService.fetchCategoryCodes(
                pageNo, numOfRows, lclsSystm1, lclsSystm2, lclsSystm3, lclsSystmListYn
        );
        return ResponseEntity.ok(codes);
    }
}