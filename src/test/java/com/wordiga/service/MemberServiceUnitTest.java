package com.wordiga.service;

import com.wordiga.client.KakaoUnlinkClient;
import com.wordiga.domain.Member;
import com.wordiga.domain.OAuthProvider;
import com.wordiga.domain.Proposal;
import com.wordiga.repository.MemberRepository;
import com.wordiga.repository.ProposalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceUnitTest {
    @Mock MemberRepository memberRepository;
    @Mock ProposalRepository proposalRepository;
    @Mock ProposalStorage proposalStorage;
    @Mock KakaoUnlinkClient kakaoUnlinkClient;

    @Test void readsSocialProfile() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getEmail()).thenReturn("user@example.com");
        when(member.getNickname()).thenReturn("사용자");
        when(member.getProvider()).thenReturn(OAuthProvider.GOOGLE);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        var result = service().getProfile(1L);

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
    }

    @Test void unlinksKakaoDeletesFilesAndMember() {
        Member member = mock(Member.class);
        Proposal proposal = mock(Proposal.class);
        when(member.getProvider()).thenReturn(OAuthProvider.KAKAO);
        when(member.getProviderId()).thenReturn("12345");
        when(proposal.getS3Key()).thenReturn("proposals/1/file.docx");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(proposalRepository.findByPlanMemberId(1L)).thenReturn(List.of(proposal));

        service().withdraw(1L);

        verify(kakaoUnlinkClient).unlink("12345");
        verify(proposalStorage).delete("proposals/1/file.docx");
        verify(memberRepository).delete(member);
    }

    @Test void googleWithdrawalUsesClientSideUnlinkAndDeletesMember() {
        Member member = mock(Member.class);
        when(member.getProvider()).thenReturn(OAuthProvider.GOOGLE);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(proposalRepository.findByPlanMemberId(1L)).thenReturn(List.of());

        service().withdraw(1L);

        verifyNoInteractions(kakaoUnlinkClient, proposalStorage);
        verify(memberRepository).delete(member);
    }

    @Test void rejectsUnknownMember() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().withdraw(1L)).hasMessageContaining("404");
        verifyNoInteractions(kakaoUnlinkClient, proposalStorage, proposalRepository);
    }

    private MemberService service() {
        return new MemberService(memberRepository, proposalRepository, proposalStorage, kakaoUnlinkClient);
    }
}
