package com.wordiga.tourism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordiga.global.config.SecurityConfig;
import com.wordiga.global.security.JwtAuthenticationFilter;
import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.global.security.OidcLoginSuccessHandler;
import com.wordiga.global.security.RestAuthenticationEntryPoint;
import com.wordiga.member.repository.MemberRepository;
import com.wordiga.member.service.CustomOidcUserService;
import com.wordiga.tourism.service.TourismContentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TourismCategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class TourismCategoryAuthenticationControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean TourismContentService tourismContentService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CustomOidcUserService customOidcUserService;
    @MockitoBean OidcLoginSuccessHandler oidcLoginSuccessHandler;
    @MockitoBean MemberRepository memberRepository;

    @Test
    void allowsCategoryMetadataWithoutJwt() throws Exception {
        when(tourismContentService.getCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tourism/categories"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class JacksonConfig {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }
}
