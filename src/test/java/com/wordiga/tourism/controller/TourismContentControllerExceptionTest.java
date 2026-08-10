package com.wordiga.tourism.controller;

import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.repository.MemberRepository;
import com.wordiga.tourism.service.TourismContentDetailService;
import com.wordiga.tourism.service.TourismContentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TourismContentController.class)
@AutoConfigureMockMvc(addFilters = false)
class TourismContentControllerExceptionTest {

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
    void rejectsPageSizeOverLimit() throws Exception {
        mockMvc.perform(get("/api/v1/tourism/contents")
                        .param("size", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsParticipantCountOverFifty() throws Exception {
        mockMvc.perform(get("/api/v1/tourism/contents/126508")
                        .param("participantCount", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidVisitDateFormat() throws Exception {
        mockMvc.perform(get("/api/v1/tourism/contents/126508")
                        .param("visitDate", "2026/08/20"))
                .andExpect(status().isBadRequest());
    }
}
