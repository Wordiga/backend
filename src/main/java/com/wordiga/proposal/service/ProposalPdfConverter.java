package com.wordiga.proposal.service;


import lombok.extern.slf4j.Slf4j;
import org.docx4j.Docx4J;
import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.fonts.PhysicalFont;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@Slf4j
@Component
public class ProposalPdfConverter {

    private static final String[] KOREAN_FONT_CANDIDATES = {
            "NanumGothic", "나눔고딕",
            "Apple SD Gothic Neo", "AppleSDGothicNeo-Regular", "AppleGothic",
            "Malgun Gothic", "맑은 고딕"
    };

    private static final String[] DOCX_KOREAN_FONT_NAMES = {
            "맑은 고딕", "Malgun Gothic",
            "바탕", "Batang",
            "굴림", "Gulim",
            "돋움", "Dotum",
            "나눔고딕", "NanumGothic",
            "Apple SD Gothic Neo"
    };

    private static final String[] SYMBOL_FONT_CANDIDATES = {
            "Symbol", "Wingdings", "Wingdings 2", "Wingdings 3",
            "Webdings", "Arial Unicode MS"
    };

    public byte[] convert(byte[] docxBytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(docxBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.load(in);

            IdentityPlusMapper fontMapper = new IdentityPlusMapper();
            Map<String, PhysicalFont> systemFonts = PhysicalFonts.getPhysicalFonts();

            // 한글 폰트 매핑
            PhysicalFont koreanFallback = findFont(systemFonts, KOREAN_FONT_CANDIDATES);
            if (koreanFallback != null) {
                log.info("PDF 한글 fallback 폰트: {}", koreanFallback.getName());
                for (String name : DOCX_KOREAN_FONT_NAMES) {
                    fontMapper.put(name, koreanFallback);
                }
            } else {
                log.warn("시스템에서 한글 폰트를 찾을 수 없습니다.");
            }

            // 심볼/bullet 폰트 매핑
            for (String symbolFont : SYMBOL_FONT_CANDIDATES) {
                PhysicalFont found = systemFonts.get(symbolFont);
                if (found != null) {
                    fontMapper.put(symbolFont, found);
                    log.debug("심볼 폰트 매핑: {}", symbolFont);
                } else if (koreanFallback != null) {
                    fontMapper.put(symbolFont, koreanFallback);
                    log.debug("심볼 폰트 {} → 한글 폰트로 대체", symbolFont);
                }
            }

            wordPackage.setFontMapper(fontMapper);
            Docx4J.toPDF(wordPackage, out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("docx → pdf 변환 실패", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 변환에 실패했습니다.", e);
        }
    }

    private PhysicalFont findFont(Map<String, PhysicalFont> systemFonts, String[] candidates) {
        for (String candidate : candidates) {
            PhysicalFont font = systemFonts.get(candidate);
            if (font != null) return font;
        }
        for (Map.Entry<String, PhysicalFont> entry : systemFonts.entrySet()) {
            String name = entry.getKey().toLowerCase();
            if (name.contains("nanum") || (name.contains("gothic") && name.contains("apple"))) {
                return entry.getValue();
            }
        }
        return null;
    }
}