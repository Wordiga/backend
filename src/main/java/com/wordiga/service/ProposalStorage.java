package com.wordiga.service;

import com.wordiga.config.ProposalS3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component @RequiredArgsConstructor
public class ProposalStorage {
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private final S3Client s3Client; private final S3Presigner presigner; private final ProposalS3Properties properties;
    public void put(String key, byte[] bytes) {
        configured();
        try { s3Client.putObject(PutObjectRequest.builder().bucket(properties.bucket()).key(key).contentType(DOCX).build(), RequestBody.fromBytes(bytes)); }
        catch (S3Exception e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "제안서를 저장할 수 없습니다.", e); }
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
        try { s3Client.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucket()).key(key).build()); }
        catch (RuntimeException ignored) { }
    }
    private void configured() {
        if (properties.bucket() == null || properties.bucket().isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "제안서 저장소가 설정되지 않았습니다.");
    }
}
