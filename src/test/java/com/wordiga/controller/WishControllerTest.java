package com.wordiga.controller;

import com.wordiga.dto.wish.WishFolderResponse;
import com.wordiga.dto.wish.WishResponse;
import com.wordiga.global.security.JwtTokenProvider;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishController.class)
@AutoConfigureMockMvc(addFilters = false)
class WishControllerTest {

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
    void addsAndDeletesWish() throws Exception {
        when(wishService.addWish(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(WishResponse.builder().contentId("126508").folderName("아산시").build());

        mockMvc.perform(post("/api/v1/wishes")
                        .contentType("application/json")
                        .content("""
                                {"contentId":"126508"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value("126508"))
                .andExpect(jsonPath("$.folderName").value("아산시"));

        mockMvc.perform(delete("/api/v1/wishes")
                        .contentType("application/json")
                        .content("""
                                {"contentId":"126508"}
                                """))
                .andExpect(status().isOk());
        verify(wishService).removeWish(1L, "126508");
    }

    @Test
    void returnsFoldersAndFolderContents() throws Exception {
        when(wishService.getWishFolders(1L)).thenReturn(List.of(
                WishFolderResponse.builder().folderName("아산시").count(1L).build()));
        when(wishService.getWishesByFolder(1L, "아산시")).thenReturn(List.of(
                WishResponse.builder().contentId("126508").build()));

        mockMvc.perform(get("/api/v1/wishes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].folderName").value("아산시"));
        mockMvc.perform(get("/api/v1/wishes/아산시"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contentId").value("126508"));
    }
}
