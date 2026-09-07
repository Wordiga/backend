package com.wordiga.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordiga.auth.dto.AccessTokenResponse;
import com.wordiga.auth.service.AuthService;
import com.wordiga.global.config.SecurityConfig;
import com.wordiga.global.security.JwtAuthenticationFilter;
import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.global.security.OidcLoginSuccessHandler;
import com.wordiga.global.security.RestAuthenticationEntryPoint;
import com.wordiga.member.repository.MemberRepository;
import com.wordiga.member.service.CustomOidcUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class AuthAuthenticationControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AuthService authService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CustomOidcUserService customOidcUserService;
    @MockitoBean OidcLoginSuccessHandler oidcLoginSuccessHandler;
    @MockitoBean MemberRepository memberRepository;

    @Test
    void allowsRefreshWithoutAccessToken() throws Exception {
        when(authService.refresh("refresh")).thenReturn(new AccessTokenResponse("access"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @TestConfiguration
    static class JacksonConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
