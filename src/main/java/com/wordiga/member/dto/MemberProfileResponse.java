package com.wordiga.member.dto;

import com.wordiga.member.Member;
import com.wordiga.member.OAuthProvider;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
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
