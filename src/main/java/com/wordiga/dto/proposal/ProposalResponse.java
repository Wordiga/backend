package com.wordiga.dto.proposal;

import com.wordiga.domain.Proposal;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class ProposalResponse {
    private Long proposalId; private Long planId; private String fileName; private long fileSize;
    private String previewUrl; private String downloadUrl; private LocalDateTime createdAt; private LocalDateTime expiresAt;
    public static ProposalResponse from(Proposal p, String previewUrl, String downloadUrl) {
        return builder().proposalId(p.getId()).planId(p.getPlan().getId()).fileName(p.getFileName())
                .fileSize(p.getFileSize()).previewUrl(previewUrl).downloadUrl(downloadUrl)
                .createdAt(p.getCreatedAt()).expiresAt(p.getExpiresAt()).build();
    }
}
