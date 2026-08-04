package com.wordiga.service;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.assertj.core.api.Assertions.*;

class ProposalServiceUnitExceptionTest {
    private final ProposalService service = new ProposalService(null, null, null, null, null, null);

    @Test void acceptsDocxPackageAndRejectsArbitraryBytes() throws Exception {
        service.validateDocx(docx());
        assertThatThrownBy(() -> service.validateDocx("not-docx".getBytes())).hasMessageContaining("502");
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
