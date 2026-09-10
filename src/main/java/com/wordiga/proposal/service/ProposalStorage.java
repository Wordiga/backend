package com.wordiga.proposal.service;

import com.wordiga.global.config.ProposalS3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProposalStorage {
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PDF = "application/pdf";
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final ProposalS3Properties properties;

    public void put(String key, byte[] bytes) {
        putWithContentType(key, bytes, DOCX);
    }

    public void putPdf(String key, byte[] bytes) {
        putWithContentType(key, bytes, PDF);
    }

    private void putWithContentType(String key, byte[] bytes, String contentType) {
        configured();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(properties.bucket()).key(key).contentType(contentType).build(),
                    RequestBody.fromBytes(bytes));
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "제안서를 저장할 수 없습니다.", e);
        }
    }

    public String url(String key, String fileName, boolean download) {
        configured();
        GetObjectRequest get = GetObjectRequest.builder().bucket(properties.bucket()).key(key)
                .responseContentDisposition((download ? "attachment" : "inline") + "; filename*=UTF-8''"
                        + URLEncoder.encode(fileName, StandardCharsets.UTF_8)).build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(properties.urlValidity())
                .getObjectRequest(get).build()).url().toString();
    }

    public void delete(String key) {
        configured();
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucket()).key(key).build());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "제안서를 삭제할 수 없습니다.", exception);
        }
    }

    public void deleteQuietly(String key) {
        try {
            delete(key);
        } catch (RuntimeException exception) {
            log.warn("제안서 보상 삭제에 실패했습니다. key={}", key, exception);
        }
    }

    private void configured() {
        if (properties.bucket() == null || properties.bucket().isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "제안서 저장소가 설정되지 않았습니다.");
    }
}