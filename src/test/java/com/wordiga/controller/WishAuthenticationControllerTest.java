package com.wordiga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordiga.global.config.SecurityConfig;
import com.wordiga.global.security.JwtAuthenticationFilter;
import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.global.security.OidcLoginSuccessHandler;
import com.wordiga.global.security.RestAuthenticationEntryPoint;
import com.wordiga.repository.MemberRepository;
import com.wordiga.service.CustomOidcUserService;
import com.wordiga.service.WishService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WishController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class WishAuthenticationControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    WishService wishService;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    CustomOidcUserService customOidcUserService;
    @MockitoBean
    OidcLoginSuccessHandler oidcLoginSuccessHandler;
    @MockitoBean
    MemberRepository memberRepository;

    @Test
    void returnsCommonUnauthorizedResponseWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/wishes"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인해 주세요."))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @TestConfiguration
    static class JacksonConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
