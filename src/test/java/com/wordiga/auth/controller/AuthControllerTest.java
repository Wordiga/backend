package com.wordiga.auth.controller;

import com.wordiga.auth.dto.AccessTokenResponse;
import com.wordiga.auth.service.AuthService;
import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AuthService authService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean MemberRepository memberRepository;

    @Test
    void refreshesAccessTokenWithoutCachingResponse() throws Exception {
        when(authService.refresh("refresh")).thenReturn(new AccessTokenResponse("access"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void logoutExpiresRefreshCookie() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/session"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("refreshToken="),
                        org.hamcrest.Matchers.containsString("Max-Age=0"),
                        org.hamcrest.Matchers.containsString("HttpOnly"))));
    }
}
