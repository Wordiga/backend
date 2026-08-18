package com.wordiga.member.controller;

import com.wordiga.member.api.MemberApi;
import com.wordiga.member.dto.MemberProfileResponse;
import com.wordiga.global.security.CurrentMemberId;
import com.wordiga.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MemberController implements MemberApi {
    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<MemberProfileResponse> getProfile(@CurrentMemberId Long memberId) {
        return ResponseEntity.ok(memberService.getProfile(memberId));
    }

    @DeleteMapping
    public ResponseEntity<Void> withdraw(@CurrentMemberId Long memberId) {
        memberService.withdraw(memberId);
        return ResponseEntity.noContent().build();
    }
}
