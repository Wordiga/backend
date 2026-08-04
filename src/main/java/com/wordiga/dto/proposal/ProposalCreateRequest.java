package com.wordiga.dto.proposal;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProposalCreateRequest {
    @Size(max = 100) private String proposalTitle;
    @Size(max = 100) private String organizationName;
    @Size(max = 1000) private String purpose;
    @Size(max = 1000) private String additionalRequest;
}
