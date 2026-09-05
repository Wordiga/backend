package com.wordiga.tourism.controller;

import com.wordiga.tourism.dto.TourismContentDto;
import com.wordiga.tourism.dto.TourismContentListResponse;
import com.wordiga.tourism.dto.SigunguResponse;
import com.wordiga.tourism.dto.detail.TourismCommonDetailDto;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.member.repository.MemberRepository;
import com.wordiga.tourism.service.TourismContentDetailService;
import com.wordiga.tourism.service.TourismContentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TourismContentController.class, TourismCategoryController.class})
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

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void returnsTourismContentList() throws Exception {
        TourismContentDto item = TourismContentDto.builder()
                .contentId("126508")
                .title("현충사")
                .isWished(true)
                .build();
        when(tourismContentService.getContentList(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(0), eq(10)))
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
                .andExpect(jsonPath("$.items[0].isWished").value(true))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void returnsSixteenChungnamSigungu() throws Exception {
        List<SigunguResponse> sigungus = new java.util.ArrayList<>(List.of(
                new SigunguResponse("110", "천안시 동남구"),
                new SigunguResponse("120", "천안시 서북구")));
        IntStream.range(2, 16).forEach(index ->
                sigungus.add(new SigunguResponse(String.valueOf(index), "시군구 " + index)));
        when(tourismContentService.getSigunguList()).thenReturn(sigungus);

        mockMvc.perform(get("/api/v1/tourism/contents/sigungu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16))
                .andExpect(jsonPath("$[0].name").value("천안시 동남구"))
                .andExpect(jsonPath("$[1].name").value("천안시 서북구"));
    }

    @Test
    void returnsCategoryGroups() throws Exception {
        when(tourismContentService.getCategories()).thenReturn(List.of(
                new com.wordiga.tourism.dto.TourismCategoryGroupDto(
                        new com.wordiga.tourism.dto.CodeNameDto("ATTRACTION_EXPERIENCE", "관광지/체험"),
                        List.of(new com.wordiga.tourism.dto.CodeNameDto("EV", "축제/공연/행사")))));

        mockMvc.perform(get("/api/v1/tourism/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].theme.code").value("ATTRACTION_EXPERIENCE"))
                .andExpect(jsonPath("$[0].theme.name").value("관광지/체험"))
                .andExpect(jsonPath("$[0].categories[0].code").value("EV"));
    }

    @Test
    void returnsIntegratedDetail() throws Exception {
        TourismCommonDetailDto common = TourismCommonDetailDto.builder()
                .contentId("126508")
                .title("현충사")
                .build();
        when(tourismContentDetailService.getDetail(eq("126508"), any(), any(), any(), any()))
                .thenReturn(TourismContentDetailResponse.builder()
                        .common(common)
                        .details(List.of())
                        .images(List.of())
                        .seasonalImages(List.of())
                        .build());

        mockMvc.perform(get("/api/v1/tourism/contents/126508")
                        .param("visitDate", "2026-08-20")
                        .param("ageGroups", "20S,30S")
                        .param("stayDays", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.common.contentId").value("126508"))
                .andExpect(jsonPath("$.common.title").value("현충사"));
        verify(tourismContentDetailService).getDetail("126508", LocalDate.of(2026, 8, 20),
                List.of("20S", "30S"), 1, 10);

        mockMvc.perform(get("/api/v1/tourism/contents/126508"))
                .andExpect(status().isOk());
    }
}
