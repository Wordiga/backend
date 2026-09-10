package com.wordiga.proposal.repository;

import com.wordiga.proposal.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    Optional<Proposal> findByIdAndPlanIdAndPlanMemberId(Long id, Long planId, Long memberId);

    Optional<Proposal> findByPlanId(Long planId);

    Optional<Proposal> findByPlanIdAndExpiresAtAfter(Long planId, LocalDateTime now);

    List<Proposal> findByPlanMemberId(Long memberId);
}
