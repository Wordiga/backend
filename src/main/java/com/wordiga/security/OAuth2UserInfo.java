package com.wordiga.security;

public interface OAuth2UserInfo {
    String getProviderId();

    String getEmail();

    String getNickname();

    String getProfileImage();
}