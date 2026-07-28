package com.wordiga.controller;

import com.wordiga.dto.tourismContent.TourismContentDto;
import com.wordiga.dto.tourismContent.TourismContentListResponse;
import com.wordiga.dto.tourismContent.detail.TourismCommonDetailDto;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import com.wordiga.security.JwtTokenProvider;
import com.wordiga.service.TourismContentDetailService;
import com.wordiga.service.TourismContentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TourismContentController.class)
@AutoConfigureMockMvc(addFilters = false)
class TourismContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TourismContentService tourismContentService;

    @MockitoBean
    private TourismContentDetailService tourismContentDetailService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void returnsTourismContentList() throws Exception {
        TourismContentDto item = TourismContentDto.builder()
                .contentId("126508")
                .title("현충사")
                .build();
        when(tourismContentService.getContentList(
                any(), any(), any(), any(), any(), eq(0), eq(10)))
                .thenReturn(TourismContentListResponse.builder()
                        .items(List.of(item))
                        .page(0)
                        .size(10)
                        .hasNext(false)
                        .build());

        mockMvc.perform(get("/api/v1/tourism/contents")
                        .param("type", "POPULAR")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].contentId").value("126508"))
                .andExpect(jsonPath("$.items[0].title").value("현충사"))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void returnsIntegratedDetail() throws Exception {
        TourismCommonDetailDto common = TourismCommonDetailDto.builder()
                .contentId("126508")
                .title("현충사")
                .build();
        when(tourismContentDetailService.getDetail(eq("126508"), any(), any()))
                .thenReturn(TourismContentDetailResponse.builder()
                        .common(common)
                        .details(List.of())
                        .images(List.of())
                        .seasonalImages(List.of())
                        .build());

        mockMvc.perform(get("/api/v1/tourism/contents/126508")
                        .param("visitDate", "2026-08-20")
                        .param("ageGroups", "20S,30S")
                        .param("maleRatio", "50")
                        .param("femaleRatio", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.common.contentId").value("126508"))
                .andExpect(jsonPath("$.common.title").value("현충사"));

        mockMvc.perform(get("/api/v1/tourism/contents/126508")
                        .param("maleRatio", "50"))
                .andExpect(status().isOk());
    }
}
