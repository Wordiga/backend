package com.wordiga.proposal.dto;

import com.wordiga.proposal.Proposal;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProposalResponse {
    private Long proposalId;
    private Long planId;
    private String fileName;
    private long fileSize;
    private String pdfUrl;
    private String docxUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static ProposalResponse from(Proposal p, String pdfUrl, String docxUrl) {
        return builder().proposalId(p.getId()).planId(p.getPlan().getId()).fileName(p.getFileName())
                .fileSize(p.getFileSize()).pdfUrl(pdfUrl).docxUrl(docxUrl)
                .createdAt(p.getCreatedAt()).expiresAt(p.getExpiresAt()).build();
    }
}
