package com.wordiga.proposal.service;

import com.wordiga.plan.Plan;
import com.wordiga.proposal.Proposal;
import com.wordiga.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProposalWriter {
    private final ProposalRepository proposalRepository;

    @Transactional(timeout = 5)
    public Proposal save(Plan plan, String key, String fileName, long size, LocalDateTime expiresAt) {
        return proposalRepository.save(Proposal.create(plan, key, fileName, size, expiresAt));
    }
}
