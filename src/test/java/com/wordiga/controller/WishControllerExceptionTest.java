package com.wordiga.controller;

import com.wordiga.security.JwtTokenProvider;
import com.wordiga.repository.MemberRepository;
import com.wordiga.service.WishService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishController.class)
@AutoConfigureMockMvc(addFilters = false)
class WishControllerExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishService wishService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private MemberRepository memberRepository;

    @BeforeEach
    void authenticateMember() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsBlankContentId() throws Exception {
        mockMvc.perform(post("/api/v1/wishes")
                        .contentType("application/json")
                        .content("""
                                {"contentId":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/wishes")
                        .contentType("application/json"))
                .andExpect(status().isBadRequest());
    }
}
