package com.wordiga.proposal.service;

import com.wordiga.global.client.AiServerClient;
import com.wordiga.global.config.KtStorageProperties;
import com.wordiga.plan.Plan;
import com.wordiga.plan.PlanContent;
import com.wordiga.plan.service.PlanReader;
import com.wordiga.proposal.Proposal;
import com.wordiga.proposal.dto.AiProposalRequest;
import com.wordiga.proposal.dto.ProposalResponse;
import com.wordiga.proposal.repository.ProposalRepository;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalServiceUnitTest {
    @Mock
    PlanReader planReader;
    @Mock
    ProposalRepository proposalRepository;
    @Mock
    AiServerClient aiServerClient;
    @Mock
    ProposalStorage storage;
    @Mock
    ProposalWriter writer;
    @Mock
    ProposalPdfConverter pdfConverter;
    ProposalService service;
    Plan plan;

    @BeforeEach
    void setUp() {
        service = new ProposalService(planReader, proposalRepository, aiServerClient, storage, writer, pdfConverter,
                new KtStorageProperties("bucket", "kr-standard", null, "accessKey", "secretKey", Duration.ofMinutes(15), Duration.ofDays(30)));
        plan = Plan.create(null, "아산 일정", LocalDate.now(), LocalDate.now(), 2);
        TourismContentSnapshot content = TourismContentSnapshot.builder()
                .contentId("1").contentTypeId("12").title("현충사")
                .updatedAt(LocalDateTime.now()).build();
        plan.addContent(PlanContent.create(plan, 1, 1, content));
    }

    @Test
    void createsAndStoresDocxAndPdfProposal() throws Exception {
        byte[] docx = docx();
        when(planReader.read(1L, 9L)).thenReturn(new PlanReader.Snapshot(plan, com.wordiga.plan.dto.PlanDetailResponse.from(plan)));
        when(aiServerClient.generateProposal(any())).thenReturn(docx);
        when(pdfConverter.convert(docx)).thenReturn("pdf".getBytes());
        when(writer.save(eq(plan), anyString(), anyString(), eq("아산 일정 제안서.docx"), eq((long) docx.length), any()))
                .thenAnswer(inv -> Proposal.create(plan, inv.getArgument(1), inv.getArgument(2),
                        inv.getArgument(3), inv.getArgument(4), inv.getArgument(5)));
        when(storage.url(anyString(), anyString(), anyBoolean())).thenReturn("https://signed");

        ProposalResponse response = service.create(1L, 9L);

        assertThat(response.getPdfUrl()).isEqualTo("https://signed");
        assertThat(response.getDocxUrl()).isEqualTo("https://signed");
        verify(storage).put(anyString(), same(docx));
        verify(storage).putPdf(anyString(), any());
        ArgumentCaptor<AiProposalRequest> payload = ArgumentCaptor.forClass(AiProposalRequest.class);
        verify(aiServerClient).generateProposal(payload.capture());

        int currentYm = Integer.parseInt(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")));
        assertThat(payload.getValue().getVisitMonth()).isEqualTo(currentYm);
        assertThat(payload.getValue().getNumPeople()).isEqualTo(2);
    }

    @Test
    void deletesOwnedProposalFromS3AndDatabase() {
        Proposal proposal = Proposal.create(plan, "proposals/1/9/file.docx", null, "제안서.docx", 10L,
                LocalDateTime.now().plusDays(30));
        when(proposalRepository.findByIdAndPlanIdAndPlanMemberId(3L, 9L, 1L)).thenReturn(Optional.of(proposal));

        service.delete(1L, 9L, 3L);

        verify(storage).delete("proposals/1/9/file.docx");
        verify(proposalRepository).delete(proposal);
    }

    @Test
    void rejectsDeletingUnknownProposal() {
        when(proposalRepository.findByIdAndPlanIdAndPlanMemberId(3L, 9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 9L, 3L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("404");
        verifyNoInteractions(storage);
    }

    private byte[] docx() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.closeEntry();
        }
        return out.toByteArray();
    }
}
