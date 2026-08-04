package com.wordiga.service;

import com.wordiga.domain.Plan;
import com.wordiga.domain.Proposal;
import com.wordiga.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class ProposalWriter {
    private final ProposalRepository proposalRepository;
    @Transactional(timeout = 5)
    public Proposal save(Plan plan, String key, String fileName, long size, LocalDateTime expiresAt) {
        return proposalRepository.save(Proposal.create(plan, key, fileName, size, expiresAt));
    }
}
