package com.wordiga.member.service;

import com.wordiga.proposal.service.ProposalStorage;

import com.wordiga.member.dto.MemberProfileResponse;
import com.wordiga.global.client.KakaoUnlinkClient;
import com.wordiga.member.Member;
import com.wordiga.member.OAuthProvider;
import com.wordiga.member.repository.MemberRepository;
import com.wordiga.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final ProposalRepository proposalRepository;
    private final ProposalStorage proposalStorage;
    private final KakaoUnlinkClient kakaoUnlinkClient;

    public MemberProfileResponse getProfile(Long memberId) {
        return MemberProfileResponse.from(member(memberId));
    }

    public void withdraw(Long memberId) {
        Member member = member(memberId);
        if (member.getProvider() == OAuthProvider.KAKAO) kakaoUnlinkClient.unlink(member.getProviderId());
        proposalRepository.findByPlanMemberId(memberId)
                .forEach(proposal -> {
                    proposalStorage.delete(proposal.getS3Key());
                    if (proposal.getS3KeyPdf() != null) proposalStorage.delete(proposal.getS3KeyPdf());
                });
        memberRepository.delete(member);
    }

    private Member member(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }
}
