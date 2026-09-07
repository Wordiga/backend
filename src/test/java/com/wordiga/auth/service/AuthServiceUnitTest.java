package com.wordiga.auth.service;

import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceUnitTest {
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final AuthService service = new AuthService(tokens, members);

    @Test
    void issuesAccessTokenOnlyForValidRefreshTokenAndExistingMember() {
        when(tokens.isValidRefresh("refresh")).thenReturn(true);
        when(tokens.getMemberId("refresh")).thenReturn(1L);
        when(members.existsById(1L)).thenReturn(true);
        when(tokens.create(1L)).thenReturn("access");

        assertThat(service.refresh("refresh").accessToken()).isEqualTo("access");
    }

    @Test
    void rejectsMissingInvalidAndWithdrawnMemberTokens() {
        assertThatThrownBy(() -> service.refresh(null)).hasMessageContaining("401");
        verifyNoInteractions(members);

        when(tokens.isValidRefresh("invalid")).thenReturn(false);
        assertThatThrownBy(() -> service.refresh("invalid")).hasMessageContaining("401");

        when(tokens.isValidRefresh("withdrawn")).thenReturn(true);
        when(tokens.getMemberId("withdrawn")).thenReturn(9L);
        assertThatThrownBy(() -> service.refresh("withdrawn")).hasMessageContaining("401");
    }
}
