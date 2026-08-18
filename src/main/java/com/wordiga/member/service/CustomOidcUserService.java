package com.wordiga.member.service;

import com.wordiga.member.Member;
import com.wordiga.member.OAuthProvider;
import com.wordiga.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final MemberRepository memberRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(request);

        OAuthProvider provider = extractProvider(request);
        String providerId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String nickname = resolveNickname(oidcUser, provider);
        String profileImage = resolveProfileImage(oidcUser, provider);

        memberRepository.findByProviderAndProviderId(provider, providerId)
                .ifPresentOrElse(
                        member -> member.updateProfile(nickname, profileImage),
                        () -> memberRepository.save(Member.create(email, nickname, provider, providerId, profileImage))
                );

        return oidcUser;
    }

    private OAuthProvider extractProvider(OidcUserRequest request) {
        String registrationId = request.getClientRegistration().getRegistrationId();
        return OAuthProvider.valueOf(registrationId.toUpperCase());
    }

    private String resolveNickname(OidcUser oidcUser, OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> oidcUser.getFullName();
            case KAKAO -> {
                String nickname = oidcUser.getAttribute("nickname");
                yield nickname != null ? nickname : oidcUser.getFullName();
            }
        };
    }

    private String resolveProfileImage(OidcUser oidcUser, OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> oidcUser.getPicture();
            case KAKAO -> oidcUser.getAttribute("picture");
        };
    }
}