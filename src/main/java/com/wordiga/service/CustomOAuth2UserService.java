package com.wordiga.service;

import com.wordiga.domain.Member;
import com.wordiga.domain.OAuthProvider;
import com.wordiga.repository.MemberRepository;
import com.wordiga.security.GoogleUserInfo;
import com.wordiga.security.KakaoUserInfo;
import com.wordiga.security.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;
    private final DefaultOAuth2UserService defaultOAuth2UserService = new DefaultOAuth2UserService();
    private final OidcUserService oidcUserService = new OidcUserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2User oAuth2User;

        if (userRequest instanceof OidcUserRequest oidcUserRequest) {
            oAuth2User = oidcUserService.loadUser(oidcUserRequest);
        } else if ("kakao".equalsIgnoreCase(registrationId) && userRequest.getClientRegistration().getScopes().contains("openid")) {
            oAuth2User = defaultOAuth2UserService.loadUser(userRequest);
        } else {
            oAuth2User = defaultOAuth2UserService.loadUser(userRequest);
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserInfo userInfo = createOAuth2UserInfo(registrationId, attributes);
        Member member = saveOrUpdate(userInfo, registrationId);

        Map<String, Object> modifiedAttributes = new HashMap<>(attributes);
        modifiedAttributes.put("memberId", member.getId());
        modifiedAttributes.put("email", member.getEmail());
        modifiedAttributes.put("nickname", member.getNickname());
        modifiedAttributes.put("profileImage", member.getProfileImage());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                modifiedAttributes,
                "memberId"
        );
    }

    private OAuth2UserInfo createOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if ("google".equalsIgnoreCase(registrationId)) return new GoogleUserInfo(attributes);
        if ("kakao".equalsIgnoreCase(registrationId)) return new KakaoUserInfo(attributes);
        throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
    }

    private Member saveOrUpdate(OAuth2UserInfo userInfo, String registrationId) {
        OAuthProvider provider = OAuthProvider.valueOf(registrationId.toUpperCase());
        return memberRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
                .orElseGet(() -> memberRepository.save(Member.create(
                        userInfo.getEmail(),
                        userInfo.getNickname(),
                        provider,
                        userInfo.getProviderId(),
                        userInfo.getProfileImage()
                )));
    }
}