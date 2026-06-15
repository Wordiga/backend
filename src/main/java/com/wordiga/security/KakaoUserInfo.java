package com.wordiga.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        if (attributes.get("sub") != null) {
            return String.valueOf(attributes.get("sub"));
        }
        if (attributes.get("id") != null) {
            return String.valueOf(attributes.get("id"));
        }
        return null;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() {
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileImage() {
        if (attributes.get("picture") != null) return (String) attributes.get("picture");

        Object kakaoAccountObj = attributes.get("kakao_account");
        if (kakaoAccountObj != null) {
            Map<String, Object> kakaoAccount = objectMapper.convertValue(
                    kakaoAccountObj,
                    new TypeReference<>() {
                    }
            );

            Object profileObj = kakaoAccount.get("profile");
            if (profileObj != null) {
                Map<String, Object> profile = objectMapper.convertValue(
                        profileObj,
                        new TypeReference<>() {
                        }
                );
                return (String) profile.get("profile_image_url");
            }
        }
        return null;
    }
}