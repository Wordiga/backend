package com.wordiga.proposal.repository;

import com.wordiga.proposal.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByPlanIdAndPlanMemberIdAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
            Long planId, Long memberId, LocalDateTime now);

    java.util.Optional<Proposal> findByIdAndPlanIdAndPlanMemberId(Long id, Long planId, Long memberId);

    List<Proposal> findByPlanMemberId(Long memberId);
}
