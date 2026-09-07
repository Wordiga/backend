package com.wordiga.global.security;

import com.wordiga.member.Member;
import com.wordiga.member.OAuthProvider;
import com.wordiga.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OidcLoginSuccessHandlerUnitTest {
    @Test
    void sendsAccessTokenInLegacyCallbackAndRefreshTokenOnlyInHttpOnlyCookie() throws Exception {
        JwtTokenProvider tokens = mock(JwtTokenProvider.class);
        MemberRepository members = mock(MemberRepository.class);
        OidcUser user = mock(OidcUser.class);
        Member member = mock(Member.class);
        when(user.getSubject()).thenReturn("google-user");
        when(member.getId()).thenReturn(1L);
        when(members.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-user"))
                .thenReturn(Optional.of(member));
        when(tokens.create(1L)).thenReturn("access");
        when(tokens.createRefresh(1L)).thenReturn("refresh");
        when(tokens.getRefreshExpiration()).thenReturn(2_592_000_000L);
        OidcLoginSuccessHandler handler = new OidcLoginSuccessHandler(tokens, members);
        ReflectionTestUtils.setField(handler, "frontendUrl", "https://wordiga.site");
        ReflectionTestUtils.setField(handler, "cookieSecure", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationToken(user, List.of(), "google"));

        assertThat(response.getRedirectedUrl()).isEqualTo("https://wordiga.site/login/callback?token=access");
        assertThat(response.getHeader("Set-Cookie")).contains(
                "refreshToken=refresh", "Path=/api/v1/auth", "Max-Age=2592000", "Secure", "HttpOnly", "SameSite=Lax");
        assertThat(response.getHeader("Set-Cookie")).doesNotContain("access");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }
}
