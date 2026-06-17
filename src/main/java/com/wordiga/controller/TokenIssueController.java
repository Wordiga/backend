package com.wordiga.controller;

import com.wordiga.domain.Member;
import com.wordiga.domain.OAuthProvider;
import com.wordiga.repository.MemberRepository;
import com.wordiga.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Auth", description = "인증/토큰 발급 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TokenIssueController {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    /**
     * 테스트용: memberId로 JWT 토큰 발급
     * (운영 시 제거하거나 ADMIN 권한 제한 필요)
     */
    @Operation(summary = "테스트용 JWT 토큰 발급",
            description = "DB에 존재하는 회원 ID로 JWT 토큰을 발급합니다. Swagger Authorize에 붙여넣어 사용하세요.",
            security = {})  // 인증 불필요
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> issueToken(
            @Parameter(description = "회원 ID (DB PK)") @RequestParam Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElse(null);

        if (member == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "회원을 찾을 수 없습니다.",
                    "memberId", memberId
            ));
        }

        String token = jwtTokenProvider.createToken(member.getId(), member.getEmail());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "memberId", member.getId(),
                "email", member.getEmail() != null ? member.getEmail() : "",
                "nickname", member.getNickname() != null ? member.getNickname() : "",
                "expiresIn", "86400000ms (24시간)"
        ));
    }

    /**
     * 테스트용: 가입된 회원이 없을 때 테스트 회원 생성 + 토큰 발급
     */
    @Operation(summary = "테스트 회원 생성 + JWT 발급",
            description = "테스트용 회원을 생성하고 JWT 토큰을 반환합니다. 개발 환경에서만 사용하세요.",
            security = {})
    @PostMapping("/token/test")
    public ResponseEntity<Map<String, Object>> issueTestToken(
            @Parameter(description = "테스트 이메일") @RequestParam(defaultValue = "test@wordiga.com") String email,
            @Parameter(description = "테스트 닉네임") @RequestParam(defaultValue = "테스트유저") String nickname) {

        // 이미 같은 테스트 회원이 있으면 재사용
        Member member = memberRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "test-provider-id")
                .orElseGet(() -> memberRepository.save(Member.create(
                        email,
                        nickname,
                        OAuthProvider.GOOGLE,
                        "test-provider-id",
                        ""
                )));

        String token = jwtTokenProvider.createToken(member.getId(), member.getEmail());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "memberId", member.getId(),
                "email", member.getEmail(),
                "nickname", member.getNickname(),
                "usage", "Swagger Authorize 버튼 → 이 token 값을 붙여넣으세요"
        ));
    }
}