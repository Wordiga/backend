package com.wordiga.dto.member;

import com.wordiga.domain.Member;
import com.wordiga.domain.OAuthProvider;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class MemberProfileResponse {
    private Long memberId;
    private String email;
    private String nickname;
    private String profileImage;
    private OAuthProvider provider;

    public static MemberProfileResponse from(Member member) {
        return MemberProfileResponse.builder().memberId(member.getId()).email(member.getEmail())
                .nickname(member.getNickname()).profileImage(member.getProfileImage())
                .provider(member.getProvider()).build();
    }
}
