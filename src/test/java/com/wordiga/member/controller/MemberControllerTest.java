package com.wordiga.member.controller;

import com.wordiga.member.dto.MemberProfileResponse;
import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.member.OAuthProvider;
import com.wordiga.member.repository.MemberRepository;
import com.wordiga.member.service.MemberService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    MemberService memberService;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    MemberRepository memberRepository;

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getsProfileAndWithdraws() throws Exception {
        when(memberService.getProfile(1L)).thenReturn(MemberProfileResponse.builder().memberId(1L)
                .email("user@example.com").nickname("사용자").provider(OAuthProvider.GOOGLE).build());

        mockMvc.perform(get("/api/v1/me")).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.providerId").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.s3Key").doesNotExist());
        mockMvc.perform(delete("/api/v1/me")).andExpect(status().isNoContent());

        verify(memberService).withdraw(1L);
    }
}
