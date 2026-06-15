package com.wordiga.controller;

import com.wordiga.domain.Member;
import com.wordiga.repository.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberRepository memberRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @GetMapping("/kakao/scopes")
    public ResponseEntity<String> getKakaoScopes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body("인증 토큰이 누락되었거나 유효하지 않습니다. 로그인을 다시 진행하십시오.");
        }

        String username = authentication.getName();

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                "kakao",
                username
        );

        if (authorizedClient == null) {
            return ResponseEntity.badRequest().body("카카오 연동 정보가 만료되었거나 찾을 수 없습니다. 식별자: " + username);
        }

        String kakaoAccessToken = authorizedClient.getAccessToken().getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + kakaoAccessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/scopes",
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("카카오 API 연동 조회 실패: " + e.getMessage());
        }
    }

    @GetMapping("/kakao/require-scope")
    public void redirectToKakaoRequiredScope(HttpServletResponse response) throws IOException {
        String customRedirectUri = "http://localhost:8080/api/v1/auth/kakao/scope-callback";

        String targetUrl = UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", customRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "profile_image")
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }

    @GetMapping("/kakao/scope-callback")
    @SuppressWarnings("unchecked")
    public void handleScopeCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        String customRedirectUri = "http://localhost:8080/api/v1/auth/kakao/scope-callback";

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
        tokenParams.add("grant_type", "authorization_code");
        tokenParams.add("client_id", kakaoClientId);
        tokenParams.add("client_secret", kakaoClientSecret);
        tokenParams.add("redirect_uri", customRedirectUri);
        tokenParams.add("code", code);

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenParams, tokenHeaders);

        ResponseEntity<Map<String, Object>> tokenResponseContainer = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                tokenRequest,
                new ParameterizedTypeReference<>() {
                }
        );

        Map<String, Object> tokenResponse = Optional.ofNullable(tokenResponseContainer.getBody())
                .orElse(Collections.emptyMap());

        String kakaoAccessToken = Optional.ofNullable(tokenResponse.get("access_token"))
                .map(Object::toString)
                .orElseThrow(() -> new OAuth2AuthenticationException("카카오 Access Token 획득에 실패했습니다."));

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.set("Authorization", "Bearer " + kakaoAccessToken);
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                userRequest,
                new ParameterizedTypeReference<>() {
                }
        );

        Map<String, Object> attributes = Optional.ofNullable(userResponse.getBody())
                .orElse(Collections.emptyMap());

        Map<String, Object> kakaoAccount = Optional.ofNullable((Map<String, Object>) attributes.get("kakao_account"))
                .orElse(Collections.emptyMap());

        Map<String, Object> profile = Optional.ofNullable((Map<String, Object>) kakaoAccount.get("profile"))
                .orElse(Collections.emptyMap());

        String profileImageUrl = Optional.ofNullable(profile.get("profile_image_url"))
                .map(Object::toString)
                .orElse("");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            throw new OAuth2AuthenticationException("만료되었거나 유효하지 않은 회원 세션입니다.");
        }
        String currentMemberId = authentication.getName();

        Member member = memberRepository.findById(Long.valueOf(currentMemberId))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        member.updateProfile(member.getNickname(), profileImageUrl);
        memberRepository.save(member);

        String returnUrl = UriComponentsBuilder.fromUriString("/callback.html")
                .queryParam("nickname", org.springframework.web.util.UriUtils.encode(member.getNickname(), java.nio.charset.StandardCharsets.UTF_8))
                .queryParam("email", org.springframework.web.util.UriUtils.encode(member.getEmail(), java.nio.charset.StandardCharsets.UTF_8))
                .queryParam("profileImage", org.springframework.web.util.UriUtils.encode(profileImageUrl, java.nio.charset.StandardCharsets.UTF_8))
                .build().toUriString();

        response.sendRedirect(returnUrl);
    }

    private static class OAuth2AuthenticationException extends RuntimeException {
        public OAuth2AuthenticationException(String message) {
            super(message);
        }
    }
}