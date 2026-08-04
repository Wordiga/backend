package com.wordiga.service;

import com.wordiga.client.AiServerClient;
import com.wordiga.config.ProposalS3Properties;
import com.wordiga.domain.Plan;
import com.wordiga.domain.Proposal;
import com.wordiga.dto.ai.AiProposalRequest;
import com.wordiga.dto.plan.PlanDetailResponse;
import com.wordiga.dto.proposal.*;
import com.wordiga.repository.PlanRepository;
import com.wordiga.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service @RequiredArgsConstructor
public class ProposalService {
    private static final int MAX_DOCX_SIZE = 20 * 1024 * 1024;
    private final PlanReader planReader; private final ProposalRepository proposalRepository;
    private final AiServerClient aiServerClient; private final ProposalStorage storage;
    private final ProposalWriter writer; private final ProposalS3Properties properties;

    public ProposalResponse create(Long memberId, Long planId, ProposalCreateRequest request) {
        PlanReader.Snapshot snapshot = planReader.read(memberId, planId);
        Plan plan = snapshot.plan();
        if (plan.getPlanContents().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일정 콘텐츠가 필요합니다.");
        byte[] bytes = aiServerClient.generateProposal(AiProposalRequest.from(UUID.randomUUID().toString(), request,
                snapshot.response()));
        validateDocx(bytes);
        String fileName = fileName(request.getProposalTitle(), plan.getTitle());
        String key = "proposals/%d/%d/%s.docx".formatted(memberId, planId, UUID.randomUUID());
        storage.put(key, bytes);
        try {
            String previewUrl = storage.url(key, fileName, false);
            String downloadUrl = storage.url(key, fileName, true);
            Proposal saved = writer.save(plan, key, fileName, bytes.length, LocalDateTime.now().plus(properties.retention()));
            return ProposalResponse.from(saved, previewUrl, downloadUrl);
        } catch (RuntimeException e) { storage.delete(key); throw e; }
    }
    @Transactional(readOnly = true, timeout = 5)
    public List<ProposalResponse> list(Long memberId, Long planId) {
        planReader.read(memberId, planId);
        return proposalRepository.findByPlanIdAndPlanMemberIdAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
                planId, memberId, LocalDateTime.now()).stream()
                .map(p -> ProposalResponse.from(p, storage.url(p.getS3Key(), p.getFileName(), false),
                        storage.url(p.getS3Key(), p.getFileName(), true))).toList();
    }
    private String fileName(String requested, String planTitle) {
        String title = requested == null || requested.isBlank() ? planTitle + " 제안서" : requested.strip();
        return title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".docx";
    }
    void validateDocx(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_DOCX_SIZE) invalid();
        boolean contentTypes = false, document = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                contentTypes |= "[Content_Types].xml".equals(entry.getName());
                document |= "word/document.xml".equals(entry.getName());
            }
        } catch (Exception e) { invalid(); }
        if (!contentTypes || !document) invalid();
    }
    private void invalid() { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI DOCX 응답이 올바르지 않습니다."); }
}
