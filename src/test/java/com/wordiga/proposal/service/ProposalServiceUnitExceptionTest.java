package com.wordiga.proposal.service;

import com.wordiga.proposal.repository.ProposalRepository;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProposalServiceUnitExceptionTest {
    private final ProposalService service = new ProposalService(null, null, null, null, null, null);

    @Test void acceptsDocxPackageAndRejectsArbitraryBytes() throws Exception {
        service.validateDocx(docx());
        assertThatThrownBy(() -> service.validateDocx("not-docx".getBytes())).hasMessageContaining("502");
    }

    @Test void rejectsProposalOwnedByAnotherMember() {
        ProposalRepository proposals = mock(ProposalRepository.class);
        when(proposals.findByIdAndPlanIdAndPlanMemberId(9L, 7L, 2L)).thenReturn(Optional.empty());
        ProposalService secured = new ProposalService(null, proposals, null, null, null, null);

        assertThatThrownBy(() -> secured.delete(2L, 7L, 9L)).hasMessageContaining("404");
        verify(proposals).findByIdAndPlanIdAndPlanMemberId(9L, 7L, 2L);
    }
    private byte[] docx() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml")); zip.write("<Types/>".getBytes()); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml")); zip.write("<document/>".getBytes()); zip.closeEntry();
        }
        return out.toByteArray();
    }
}
