package com.wordiga.proposal.service;

import com.wordiga.global.client.AiServerClient;
import com.wordiga.global.config.ProposalS3Properties;
import com.wordiga.plan.Plan;
import com.wordiga.plan.service.PlanReader;
import com.wordiga.proposal.Proposal;
import com.wordiga.proposal.dto.AiProposalRequest;
import com.wordiga.proposal.dto.ProposalResponse;
import com.wordiga.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ProposalService {
    private static final int MAX_DOCX_SIZE = 20 * 1024 * 1024;
    private final PlanReader planReader;
    private final ProposalRepository proposalRepository;
    private final AiServerClient aiServerClient;
    private final ProposalStorage storage;
    private final ProposalWriter writer;
    private final ProposalPdfConverter pdfConverter;
    private final ProposalS3Properties properties;

    public ProposalResponse create(Long memberId, Long planId) {
        PlanReader.Snapshot snapshot = planReader.read(memberId, planId);
        Plan plan = snapshot.plan();
        if (plan.getPlanContents().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일정 콘텐츠가 필요합니다.");

        byte[] docxBytes = aiServerClient.generateProposal(AiProposalRequest.from(snapshot.response()));
        validateDocx(docxBytes);

        String fileName = fileName(plan.getTitle());
        String uuid = UUID.randomUUID().toString();
        String docxKey = "proposals/%d/%d/%s.docx".formatted(memberId, planId, uuid);
        String pdfKey = "proposals/%d/%d/%s.pdf".formatted(memberId, planId, uuid);

        Optional<Proposal> existing = proposalRepository.findByPlanId(planId);

        // docx 저장
        storage.put(docxKey, docxBytes);

        // docx → pdf 변환 및 저장
        String savedPdfKey = null;
        try {
            byte[] pdfBytes = pdfConverter.convert(docxBytes);
            storage.putPdf(pdfKey, pdfBytes);
            savedPdfKey = pdfKey;
        } catch (RuntimeException | LinkageError e) {
            // pdf 변환 실패 시 docx만 제공 (pdf는 null)
            storage.deleteQuietly(pdfKey);
        }

        try {
            String docxUrl = storage.url(docxKey, fileName, true);
            String pdfFileName = fileName.replace(".docx", ".pdf");
            String pdfUrl = savedPdfKey != null ? storage.url(savedPdfKey, pdfFileName, false) : null;

            Proposal saved = existing.isPresent()
                    ? writer.replace(existing.get(), plan, docxKey, savedPdfKey, fileName, docxBytes.length,
                    LocalDateTime.now().plus(properties.retention()))
                    : writer.save(plan, docxKey, savedPdfKey, fileName, docxBytes.length,
                    LocalDateTime.now().plus(properties.retention()));
            existing.ifPresent(previous -> {
                storage.deleteQuietly(previous.getS3Key());
                if (previous.getS3KeyPdf() != null) storage.deleteQuietly(previous.getS3KeyPdf());
            });
            return ProposalResponse.from(saved, pdfUrl, docxUrl);
        } catch (RuntimeException e) {
            storage.deleteQuietly(docxKey);
            if (savedPdfKey != null) storage.deleteQuietly(savedPdfKey);
            throw e;
        }
    }

    @Transactional(timeout = 5)
    public void delete(Long memberId, Long planId, Long proposalId) {
        Proposal proposal = proposalRepository.findByIdAndPlanIdAndPlanMemberId(proposalId, planId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제안서를 찾을 수 없습니다."));
        storage.delete(proposal.getS3Key());
        if (proposal.getS3KeyPdf() != null) storage.deleteQuietly(proposal.getS3KeyPdf());
        proposalRepository.delete(proposal);
    }

    private String fileName(String planTitle) {
        return (planTitle + " 제안서").replaceAll("[\\\\/:*?\"<>|]", "_") + ".docx";
    }

    void validateDocx(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_DOCX_SIZE) invalid();
        boolean contentTypes = false, document = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                contentTypes |= "[Content_Types].xml".equals(entry.getName());
                document |= "word/document.xml".equals(entry.getName());
            }
        } catch (Exception e) {
            invalid();
        }
        if (!contentTypes || !document) invalid();
    }

    private void invalid() {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI DOCX 응답이 올바르지 않습니다.");
    }
}
