package com.wordiga.service;

import com.wordiga.client.AiServerClient;
import com.wordiga.config.ProposalS3Properties;
import com.wordiga.domain.*;
import com.wordiga.dto.proposal.*;
import com.wordiga.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.util.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalServiceUnitTest {
    @Mock PlanReader planReader; @Mock ProposalRepository proposalRepository; @Mock AiServerClient aiServerClient;
    @Mock ProposalStorage storage; @Mock ProposalWriter writer;
    ProposalService service; Plan plan;
    @BeforeEach void setUp() {
        service = new ProposalService(planReader, proposalRepository, aiServerClient, storage, writer,
                new ProposalS3Properties("bucket", "ap-northeast-2", Duration.ofMinutes(15), Duration.ofDays(30)));
        plan = Plan.create(null, "아산 일정", LocalDate.now(), LocalDate.now(), 2, null);
        plan.addContent(PlanContent.createForUpdate(plan, 1, LocalDate.now(), 1, "1", "현충사", "12", null, null, null, null));
    }
    @Test void createsStoresAndListsProposal() throws Exception {
        byte[] docx = docx(); ProposalCreateRequest request = new ProposalCreateRequest(); request.setProposalTitle("제안서");
        when(planReader.read(1L, 9L)).thenReturn(new PlanReader.Snapshot(plan, com.wordiga.dto.plan.PlanDetailResponse.from(plan)));
        when(aiServerClient.generateProposal(any())).thenReturn(docx);
        when(writer.save(eq(plan), anyString(), eq("제안서.docx"), eq((long) docx.length), any()))
                .thenAnswer(inv -> Proposal.create(plan, inv.getArgument(1), inv.getArgument(2), inv.getArgument(3), inv.getArgument(4)));
        when(storage.url(anyString(), eq("제안서.docx"), anyBoolean())).thenReturn("https://signed");

        ProposalResponse response = service.create(1L, 9L, request);

        assertThat(response.getPreviewUrl()).isEqualTo("https://signed");
        verify(storage).put(anyString(), same(docx));
        ArgumentCaptor<com.wordiga.dto.ai.AiProposalRequest> payload = ArgumentCaptor.forClass(com.wordiga.dto.ai.AiProposalRequest.class);
        verify(aiServerClient).generateProposal(payload.capture());
        assertThat(payload.getValue().getVisitMonth()).isEqualTo(LocalDate.now().getMonthValue());
        assertThat(payload.getValue().getNumPeople()).isEqualTo(2);
    }
    @Test void deletesOwnedProposalFromS3AndDatabase() {
        Proposal proposal = Proposal.create(plan, "proposals/1/9/file.docx", "제안서.docx", 10L,
                LocalDateTime.now().plusDays(30));
        when(proposalRepository.findByIdAndPlanIdAndPlanMemberId(3L, 9L, 1L)).thenReturn(Optional.of(proposal));

        service.delete(1L, 9L, 3L);

        verify(storage).delete("proposals/1/9/file.docx");
        verify(proposalRepository).delete(proposal);
    }

    @Test void rejectsDeletingUnknownProposal() {
        when(proposalRepository.findByIdAndPlanIdAndPlanMemberId(3L, 9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 9L, 3L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("404");
        verifyNoInteractions(storage);
    }
    private byte[] docx() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml")); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml")); zip.closeEntry();
        }
        return out.toByteArray();
    }
}
