package com.wordiga.global.client;

import com.wordiga.global.client.dto.*;
import com.wordiga.global.config.TourismProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourismApiClient {

    private final RestClient restClient;
    private final TourismProperties properties;

    // ─── 지역별 관광 수요 강도 ───

    public AreaTarExpDsResponse fetchExpenditureIntensity(String baseYm, String areaCd,
                                                          String signguCd, String tarExpDsIxCd) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("baseYm", baseYm);
        params.put("areaCd", areaCd);
        params.put("numOfRows", String.valueOf(properties.getApi().getDefaultNumOfRows()));
        putIfPresent(params, "signguCd", signguCd);
        putIfPresent(params, "tarExpDsIxCd", tarExpDsIxCd);

        URI uri = buildUri("AreaTarDemDsService/areaTarExpDsList", params);
        return callApi(uri, AreaTarExpDsResponse.class);
    }

    public AreaTarSjrnDsResponse fetchStayIntensity(String baseYm, String areaCd,
                                                    String signguCd, String tarSjrnDsIxCd) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("baseYm", baseYm);
        params.put("areaCd", areaCd);
        params.put("numOfRows", String.valueOf(properties.getApi().getDefaultNumOfRows()));
        putIfPresent(params, "signguCd", signguCd);
        putIfPresent(params, "tarSjrnDsIxCd", tarSjrnDsIxCd);

        URI uri = buildUri("AreaTarDemDsService/areaTarSjrnDsList", params);
        return callApi(uri, AreaTarSjrnDsResponse.class);
    }

    // ─── 지역별 관광 자원 수요 ───

    public AreaTarSvcDemResponse fetchServiceDemand(String baseYm, String areaCd,
                                                    String signguCd, String tarSvcDemIxCd) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("baseYm", baseYm);
        params.put("areaCd", areaCd);
        params.put("numOfRows", String.valueOf(properties.getApi().getDefaultNumOfRows()));
        putIfPresent(params, "signguCd", signguCd);
        putIfPresent(params, "tarSvcDemIxCd", tarSvcDemIxCd);

        URI uri = buildUri("AreaTarResDemService/areaTarSvcDemList", params);
        return callApi(uri, AreaTarSvcDemResponse.class);
    }

    public AreaCulResDemResponse fetchCulturalResourceDemand(String baseYm, String areaCd,
                                                             String signguCd, String culResDemIxCd) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("baseYm", baseYm);
        params.put("areaCd", areaCd);
        params.put("numOfRows", String.valueOf(properties.getApi().getDefaultNumOfRows()));
        putIfPresent(params, "signguCd", signguCd);
        putIfPresent(params, "culResDemIxCd", culResDemIxCd);

        URI uri = buildUri("AreaTarResDemService/areaCulResDemList", params);
        return callApi(uri, AreaCulResDemResponse.class);
    }

    // ─── 지역별 관광 다양성 ───

    public AreaTouDivResponse fetchTouristDiversity(
            String baseYm, String areaCd, String signguCd, String touDivIxCd) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("baseYm", baseYm);
        params.put("areaCd", areaCd);
        params.put("numOfRows", "100");
        putIfPresent(params, "signguCd", signguCd);
        putIfPresent(params, "touDivIxCd", touDivIxCd);

        URI uri = buildUri("AreaTarDivService/areaTouDivList", params);
        return callApi(uri, AreaTouDivResponse.class);
    }

    public AreaExpDivResponse fetchExpenditureDiversity(
            String baseYm, String areaCd, String signguCd, String expDivIxCd) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("baseYm", baseYm);
        params.put("areaCd", areaCd);
        params.put("numOfRows", "100");
        putIfPresent(params, "signguCd", signguCd);
        putIfPresent(params, "expDivIxCd", expDivIxCd);

        URI uri = buildUri("AreaTarDivService/areaExpDivList", params);
        return callApi(uri, AreaExpDivResponse.class);
    }

    // ─── 국문 관광정보 서비스 ───

    public List<AreaBasedItem> fetchAreaBasedContent(String lDongRegnCd, String lDongSignguCd, int numOfRows) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("numOfRows", String.valueOf(numOfRows));
        params.put("pageNo", "1");
        params.put("arrange", "Q");
        params.put("lDongRegnCd", lDongRegnCd);
        putIfPresent(params, "lDongSignguCd", lDongSignguCd);

        URI uri = buildUri("KorService2/areaBasedList2", params);
        AreaBasedResponse response = callApi(uri, AreaBasedResponse.class);

        if (response == null || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody().getItems().getItem() == null) {
            return Collections.emptyList();
        }
        return response.getResponse().getBody().getItems().getItem();
    }

    public AreaBasedResponse searchContent(String keyword, String contentTypeId,
                                           String lDongRegnCd, String lDongSignguCd,
                                           int pageNo, int numOfRows) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("keyword", keyword);
        params.put("pageNo", String.valueOf(pageNo));
        params.put("numOfRows", String.valueOf(numOfRows));
        params.put("arrange", "Q");
        params.put("lDongRegnCd", lDongRegnCd);
        putIfPresent(params, "contentTypeId", contentTypeId);
        putIfPresent(params, "lDongSignguCd", lDongSignguCd);

        URI uri = buildUri("KorService2/searchKeyword2", params);
        return callApi(uri, AreaBasedResponse.class);
    }

    public List<String> fetchClassificationNames(String lclsSystm1, String lclsSystm2, String lclsSystm3) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("pageNo", "1");
        params.put("numOfRows", "1000");
        params.put("lclsSystmListYn", "Y");
        putIfPresent(params, "lclsSystm1", lclsSystm1);
        putIfPresent(params, "lclsSystm2", lclsSystm2);
        putIfPresent(params, "lclsSystm3", lclsSystm3);

        ClassificationSystemResponse response = callApi(
                buildUri("KorService2/lclsSystmCode2", params), ClassificationSystemResponse.class);
        if (response.getResponse() != null && response.getResponse().getHeader() != null
                && response.getResponse().getHeader().getResultCode() != null
                && !"0000".equals(response.getResponse().getHeader().getResultCode())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "관광공사 분류체계 API를 사용할 수 없습니다.");
        }
        if (response.getResponse() == null || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody().getItems().getItem() == null) return null;

        return response.getResponse().getBody().getItems().getItem().stream()
                .filter(item -> java.util.Objects.equals(lclsSystm1, item.getLclsSystm1Cd())
                        && (lclsSystm2 == null || java.util.Objects.equals(lclsSystm2, item.getLclsSystm2Cd()))
                        && (lclsSystm3 == null || java.util.Objects.equals(lclsSystm3, item.getLclsSystm3Cd())))
                .findFirst()
                .map(item -> java.util.stream.Stream.of(
                                item.getLclsSystm1Nm(), item.getLclsSystm2Nm(), item.getLclsSystm3Nm())
                        .filter(value -> value != null && !value.isBlank()).distinct().toList())
                .filter(names -> !names.isEmpty())
                .orElse(null);
    }

    // ─── 관광 사진 갤러리 서비스 ───

    public PhotoGalleryResponse searchPhotos(String keyword) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("keyword", keyword);
        params.put("pageNo", "1");
        params.put("numOfRows", "20");
        params.put("arrange", "A");
        return callApi(buildUri("PhotoGalleryService1/gallerySearchList1", params), PhotoGalleryResponse.class);
    }

    // ─── 공통: API 호출 ───

    private <T> T callApi(URI uri, Class<T> responseType) {
        log.info("[TourismAPI] 호출: {}", uri);
        try {
            T response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(responseType);
            if (response == null) throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "관광공사 API 응답이 비어 있습니다.");
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TourismAPI] 실패: {} - {}", uri, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "관광공사 API를 사용할 수 없습니다.", e);
        }
    }

    // ─── 공통: URI 빌더 ───

    private URI buildUri(String operation, Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append(properties.getApi().getBaseUrl())
                .append("/").append(operation)
                .append("?serviceKey=").append(properties.getApi().getServiceKey())
                .append("&MobileOS=").append(properties.getApi().getMobileOs())
                .append("&MobileApp=").append(properties.getApi().getMobileApp())
                .append("&_type=json");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append("&")
                    .append(entry.getKey())
                    .append("=")
                    .append(UriUtils.encodeQueryParam(entry.getValue(), StandardCharsets.UTF_8));
        }

        return URI.create(sb.toString());
    }

    private void putIfPresent(Map<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value);
        }
    }
}
