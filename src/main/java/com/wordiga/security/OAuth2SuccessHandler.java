package com.wordiga.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        assert oAuth2User != null;
        Long memberId = (Long) oAuth2User.getAttributes().get("memberId");
        String email = (String) oAuth2User.getAttributes().get("email");
        String nickname = (String) oAuth2User.getAttributes().get("nickname");
        String profileImage = (String) oAuth2User.getAttributes().get("profileImage");

        String token = tokenProvider.createToken(memberId, email);

        String targetUrl = UriComponentsBuilder.fromUriString("/callback.html")
                .queryParam("token", token)
                .queryParam("nickname", org.springframework.web.util.UriUtils.encode(nickname, StandardCharsets.UTF_8))
                .queryParam("email", org.springframework.web.util.UriUtils.encode(email != null ? email : "", StandardCharsets.UTF_8))
                .queryParam("profileImage", org.springframework.web.util.UriUtils.encode(profileImage != null ? profileImage : "", StandardCharsets.UTF_8))
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}