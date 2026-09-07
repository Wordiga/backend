package com.wordiga.auth.service;

import com.wordiga.auth.dto.AccessTokenResponse;
import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    public AccessTokenResponse refresh(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.isValidRefresh(refreshToken)) throw unauthorized();
        try {
            Long memberId = jwtTokenProvider.getMemberId(refreshToken);
            if (!memberRepository.existsById(memberId)) throw unauthorized();
            return new AccessTokenResponse(jwtTokenProvider.create(memberId));
        } catch (NumberFormatException exception) {
            throw unauthorized();
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
    }
}
