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
    public Proposal save(Plan plan, String s3Key, String s3KeyPdf, String fileName, long size, LocalDateTime expiresAt) {
        return proposalRepository.save(Proposal.create(plan, s3Key, s3KeyPdf, fileName, size, expiresAt));
    }

    @Transactional(timeout = 5)
    public Proposal replace(Proposal existing, Plan plan, String s3Key, String s3KeyPdf,
                            String fileName, long size, LocalDateTime expiresAt) {
        proposalRepository.delete(existing);
        proposalRepository.flush();
        return proposalRepository.save(Proposal.create(plan, s3Key, s3KeyPdf, fileName, size, expiresAt));
    }
}
