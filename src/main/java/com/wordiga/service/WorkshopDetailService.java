package com.wordiga.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.DetailImageDto;
import com.wordiga.dto.DetailInfoDto;
import com.wordiga.dto.DetailIntroDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkshopDetailService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${tourism.api.base-url}")
    private String baseUrl;

    @Value("${tourism.api.service-key}")
    private String serviceKey;

    /**
     * 공통정보 조회 (detailCommon2)
     */
    public ContentDetailDto fetchCommonDetail(String contentId) {
        Map<String, String> params = putCommonParams(1, 1);
        params.put("contentId", contentId);

        URI uri = buildUri(baseUrl + "/detailCommon2", params);
        List<ContentDetailDto> items = callApi(uri, ContentDetailDto.class);
        return items.isEmpty() ? null : items.getFirst();
    }

    /**
     * 소개정보 조회 (detailIntro2)
     */
    public DetailIntroDto fetchIntroDetail(String contentId, String contentTypeId) {
        Map<String, String> params = putCommonParams(1, 1);
        params.put("contentId", contentId);
        params.put("contentTypeId", contentTypeId);

        URI uri = buildUri(baseUrl + "/detailIntro2", params);
        List<DetailIntroDto> items = callApi(uri, DetailIntroDto.class);
        return items.isEmpty() ? null : items.getFirst();
    }

    /**
     * 반복정보 조회 (detailInfo2)
     */
    public List<DetailInfoDto> fetchRepeatInfo(String contentId, String contentTypeId,
                                               int pageNo, int numOfRows) {
        Map<String, String> params = putCommonParams(pageNo, numOfRows);
        params.put("contentId", contentId);
        params.put("contentTypeId", contentTypeId);

        URI uri = buildUri(baseUrl + "/detailInfo2", params);
        return callApi(uri, DetailInfoDto.class);
    }

    /**
     * 이미지정보 조회 (detailImage2)
     */
    public List<DetailImageDto> fetchImages(String contentId, String imageYN,
                                            int pageNo, int numOfRows) {
        Map<String, String> params = putCommonParams(pageNo, numOfRows);
        params.put("contentId", contentId);
        putIfPresent(params, "imageYN", imageYN);

        URI uri = buildUri(baseUrl + "/detailImage2", params);
        return callApi(uri, DetailImageDto.class);
    }

    private <T> List<T> callApi(URI uri, Class<T> itemType) {
        log.info("[TourAPI Detail] URI: {}", uri);
        try {
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    uri, HttpMethod.GET, HttpEntity.EMPTY, String.class
            );
            String body = rawResponse.getBody();
            if (body == null || body.isBlank()) return Collections.emptyList();
            return parseItems(body, itemType);
        } catch (Exception e) {
            log.error("[TourAPI Detail] 예외: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "관광공사 API를 사용할 수 없습니다.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> parseItems(String json, Class<T> itemType) {
        try {
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<>() {
            });
            Map<String, Object> response = (Map<String, Object>) root.get("response");
            if (response == null) return Collections.emptyList();
            Map<String, Object> body = (Map<String, Object>) response.get("body");
            if (body == null) return Collections.emptyList();
            Object itemsRaw = body.get("items");
            if (itemsRaw == null || itemsRaw instanceof String) return Collections.emptyList();
            Map<String, Object> items = (Map<String, Object>) itemsRaw;
            Object itemRaw = items.get("item");
            if (itemRaw == null) return Collections.emptyList();
            if (itemRaw instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) itemRaw;
                List<T> result = new ArrayList<>();
                for (Map<String, Object> map : list) {
                    result.add(objectMapper.convertValue(map, itemType));
                }
                return result;
            } else if (itemRaw instanceof Map) {
                return List.of(objectMapper.convertValue(itemRaw, itemType));
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[TourAPI Detail 파싱] 실패: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "관광공사 API 응답을 처리할 수 없습니다.", e);
        }
    }

    private URI buildUri(String baseEndpoint, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(baseEndpoint).append("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            if ("serviceKey".equals(entry.getKey())) {
                sb.append("serviceKey=").append(entry.getValue());
            } else {
                sb.append(encodeParam(entry.getKey())).append("=").append(encodeParam(entry.getValue()));
            }
        }
        return URI.create(sb.toString());
    }

    private String encodeParam(String value) {
        return UriUtils.encode(value, StandardCharsets.UTF_8);
    }

    private void putIfPresent(Map<String, String> params, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            params.put(key, value.trim());
        }
    }

    private Map<String, String> putCommonParams(int pageNo, int numOfRows) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("serviceKey", serviceKey);
        params.put("MobileOS", "WEB");
        params.put("MobileApp", "Wordiga");
        params.put("pageNo", String.valueOf(pageNo));
        params.put("numOfRows", String.valueOf(numOfRows));
        params.put("_type", "json");
        return params;
    }
}
