package com.wordiga.global.security;

import com.wordiga.global.security.JwtAuthenticationFilter;
import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterUnitTest {
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesOnlyExistingMember() throws Exception {
        JwtTokenProvider tokens = mock(JwtTokenProvider.class);
        MemberRepository members = mock(MemberRepository.class);
        when(tokens.isValidAccess("token")).thenReturn(true);
        when(tokens.getMemberId("token")).thenReturn(1L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        new JwtAuthenticationFilter(tokens, members).doFilter(
                request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        when(members.existsById(1L)).thenReturn(true);
        new JwtAuthenticationFilter(tokens, members).doFilter(
                request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
    }
}
