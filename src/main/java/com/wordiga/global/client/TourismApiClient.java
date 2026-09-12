package com.wordiga.global.client;

import com.wordiga.global.client.dto.*;
import com.wordiga.global.config.TourismProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;
import java.util.Objects;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.wordiga.global.util.KtoUtils.extractItems;

@Slf4j
@Component
public class TourismApiClient {

    private final TourismHttpExchangeClient client;
    private final TourismProperties properties;

    public TourismApiClient(RestClient.Builder restClientBuilder, TourismProperties properties) {
        this.properties = properties;

        RestClient restClient = restClientBuilder
                .baseUrl(properties.getApi().getBaseUrl())
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        this.client = factory.createClient(TourismHttpExchangeClient.class);
    }

    // ─── 지역별 관광 수요 강도 ───

    @Cacheable(cacheNames = "tourismAnalytics", key = "'exp:' + #baseYm + ':' + #areaCd + ':' + #signguCd + ':' + #tarExpDsIxCd", sync = true)
    public AreaTarExpDsResponse fetchExpenditureIntensity(String baseYm, String areaCd,
                                                          String signguCd, String tarExpDsIxCd) {
        return execute(() -> client.fetchExpenditureIntensity(
                serviceKey(), mobileOs(), mobileApp(), "json",
                baseYm, areaCd, properties.getApi().getDefaultNumOfRows(), 1, signguCd, tarExpDsIxCd
        ));
    }

    // ─── 관광 체류 강도 ───

    @Cacheable(cacheNames = "tourismAnalytics", key = "'stay:' + #baseYm + ':' + #areaCd + ':' + #signguCd + ':' + #tarSjrnDsIxCd", sync = true)
    public AreaTarSjrnDsResponse fetchStayIntensity(String baseYm, String areaCd,
                                                    String signguCd, String tarSjrnDsIxCd) {
        return execute(() -> client.fetchStayIntensity(
                serviceKey(), mobileOs(), mobileApp(), "json",
                baseYm, areaCd, properties.getApi().getDefaultNumOfRows(), 1, signguCd, tarSjrnDsIxCd
        ));
    }

    // ─── 지역별 관광 자원 수요 ───

    @Cacheable(cacheNames = "tourismAnalytics", key = "'service:' + #baseYm + ':' + #areaCd + ':' + #signguCd + ':' + #tarSvcDemIxCd", sync = true)
    public AreaTarSvcDemResponse fetchServiceDemand(String baseYm, String areaCd,
                                                    String signguCd, String tarSvcDemIxCd) {
        return execute(() -> client.fetchServiceDemand(
                serviceKey(), mobileOs(), mobileApp(), "json",
                baseYm, areaCd, properties.getApi().getDefaultNumOfRows(), signguCd, tarSvcDemIxCd
        ));
    }

    // ─── 문화 자원 수요 ───

    @Cacheable(cacheNames = "tourismAnalytics", key = "'culture:' + #baseYm + ':' + #areaCd + ':' + #signguCd + ':' + #culResDemIxCd", sync = true)
    public AreaCulResDemResponse fetchCulturalResourceDemand(String baseYm, String areaCd,
                                                             String signguCd, String culResDemIxCd) {
        return execute(() -> client.fetchCulturalResourceDemand(
                serviceKey(), mobileOs(), mobileApp(), "json",
                baseYm, areaCd, properties.getApi().getDefaultNumOfRows(), signguCd, culResDemIxCd
        ));
    }

    // ─── 관광지 집중률 ───

    @Cacheable(cacheNames = "tourismConcentration", key = "#areaCd + ':' + #signguCd + ':' + #tAtsNm", sync = true)
    public TatsCnctrRateResponse fetchConcentrationRate(String areaCd, String signguCd, String tAtsNm) {
        return execute(() -> client.fetchConcentrationRate(
                serviceKey(), mobileOs(), mobileApp(), "json",
                areaCd, signguCd, 30, tAtsNm
        ));
    }

    // ─── 지역별 관광 다양성 ───

    public AreaTouDivResponse fetchTouristDiversity(String baseYm, String areaCd,
                                                    String signguCd, String touDivIxCd) {
        return execute(() -> client.fetchTouristDiversity(
                serviceKey(), mobileOs(), mobileApp(), "json",
                baseYm, areaCd, 100, signguCd, touDivIxCd
        ));
    }

    public AreaExpDivResponse fetchExpenditureDiversity(String baseYm, String areaCd,
                                                        String signguCd, String expDivIxCd) {
        return execute(() -> client.fetchExpenditureDiversity(
                serviceKey(), mobileOs(), mobileApp(), "json",
                baseYm, areaCd, 100, signguCd, expDivIxCd
        ));
    }

    // ─── 국문 관광정보 서비스 ───

    public List<AreaBasedItem> fetchAreaBasedContent(String lDongRegnCd, String lDongSignguCd, int numOfRows) {
        AreaBasedResponse response = execute(() -> client.fetchAreaBasedContent(
                serviceKey(), mobileOs(), mobileApp(), "json",
                numOfRows, 1, "Q", lDongRegnCd, lDongSignguCd
        ));
        return extractItems(response);
    }

    public AreaBasedResponse searchContent(String keyword, String contentTypeId,
                                           String lDongRegnCd, String lDongSignguCd,
                                           int pageNo, int numOfRows) {
        return execute(() -> client.searchContent(
                serviceKey(), mobileOs(), mobileApp(), "json",
                keyword, pageNo, numOfRows, "Q", lDongRegnCd, contentTypeId, lDongSignguCd
        ));
    }

    @Cacheable(cacheNames = "tourismCommon", key = "#contentId", sync = true)
    public ContentDetailDto fetchCommonDetail(String contentId) {
        ContentDetailResponse response = execute(() -> client.fetchCommonDetail(
                serviceKey(), mobileOs(), mobileApp(), "json", contentId, 1, 1
        ));
        List<ContentDetailDto> items = extractItems(response);
        return items.isEmpty() ? null : items.getFirst();
    }

    @Cacheable(cacheNames = "tourismIntro", key = "#contentId + ':' + #contentTypeId", sync = true)
    public DetailIntroDto fetchIntroDetail(String contentId, String contentTypeId) {
        DetailIntroResponse response = execute(() -> client.fetchIntroDetail(
                serviceKey(), mobileOs(), mobileApp(), "json", contentId, 1, 1, contentTypeId
        ));
        List<DetailIntroDto> items = extractItems(response);
        return items.isEmpty() ? null : items.getFirst();
    }

    @Cacheable(cacheNames = "tourismRepeat", key = "#contentId + ':' + #contentTypeId + ':' + #pageNo + ':' + #numOfRows", sync = true)
    public List<DetailInfoDto> fetchRepeatInfo(String contentId, String contentTypeId, int pageNo, int numOfRows) {
        DetailInfoResponse response = execute(() -> client.fetchRepeatInfo(
                serviceKey(), mobileOs(), mobileApp(), "json", contentId, pageNo, numOfRows, contentTypeId
        ));
        return extractItems(response);
    }

    @Cacheable(cacheNames = "tourismImages", key = "#contentId + ':' + #imageYN + ':' + #pageNo + ':' + #numOfRows", sync = true)
    public List<DetailImageDto> fetchImages(String contentId, String imageYN, int pageNo, int numOfRows) {
        DetailImageResponse response = execute(() -> client.fetchImages(
                serviceKey(), mobileOs(), mobileApp(), "json", contentId, pageNo, numOfRows, imageYN
        ));
        return extractItems(response);
    }

    public List<RelatedTourismItem> fetchRelatedTourism(String baseYm, String areaCd, String signguCd,
                                                        String keyword, int numOfRows) {
        RelatedTourismResponse response = execute(() -> client.fetchRelatedTourism(
                serviceKey(), mobileOs(), mobileApp(), "json", baseYm, areaCd, signguCd, keyword, 1, numOfRows
        ));

        if (response != null && response.getResponse() != null && response.getResponse().getHeader() != null
                && !"0000".equals(response.getResponse().getHeader().getResultCode())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관광공사 연관 관광지 API를 사용할 수 없습니다.");
        }
        return extractItems(response);
    }

    public List<String> fetchClassificationNames(String lclsSystm1, String lclsSystm2, String lclsSystm3) {
        ClassificationSystemResponse response = execute(() -> client.fetchClassificationNames(
                serviceKey(), mobileOs(), mobileApp(), "json", 1, 1000, "Y", lclsSystm1, lclsSystm2, lclsSystm3
        ));

        if (response != null && response.getResponse() != null && response.getResponse().getHeader() != null
                && !"0000".equals(response.getResponse().getHeader().getResultCode())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관광공사 분류체계 API를 사용할 수 없습니다.");
        }

        List<ClassificationSystemItem> items = extractItems(response);
        if (items.isEmpty()) return null;

        return items.stream()
                .filter(item -> Objects.equals(lclsSystm1, item.getLclsSystm1Cd())
                        && (lclsSystm2 == null || Objects.equals(lclsSystm2, item.getLclsSystm2Cd()))
                        && (lclsSystm3 == null || Objects.equals(lclsSystm3, item.getLclsSystm3Cd())))
                .findFirst()
                .map(item -> java.util.stream.Stream.of(
                                item.getLclsSystm1Nm(), item.getLclsSystm2Nm(), item.getLclsSystm3Nm())
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList())
                .filter(names -> !names.isEmpty())
                .orElse(null);
    }

    public List<SigunguItem> fetchSigunguList() {
        SigunguResponse response = execute(() -> client.fetchSigunguList(
                serviceKey(), mobileOs(), mobileApp(), "json", 1, 50,
                properties.getRegion().getChungnamCode(), "Y"
        ));
        return extractItems(response);
    }

    // ─── 관광 사진 갤러리 서비스 ───

    public PhotoGalleryResponse searchPhotos(String keyword) {
        return execute(() -> client.searchPhotos(
                serviceKey(), mobileOs(), mobileApp(), "json", keyword, 1, 20, "A"
        ));
    }

    // ─── Helper Methods ───

    private <T> T execute(ApiSupplier<T> supplier) {
        validateServiceKey();
        try {
            T response = supplier.get();
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관광공사 API 응답이 비어 있습니다.");
            }
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TourismAPI] 호출 실패: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관광공사 API를 사용할 수 없습니다.", e);
        }
    }

    private void validateServiceKey() {
        if (!StringUtils.hasText(properties.getApi().getServiceKey())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관광공사 API 서비스 키가 설정되지 않았습니다.");
        }
    }

    private String serviceKey() {
        return URLDecoder.decode(properties.getApi().getServiceKey(), StandardCharsets.UTF_8);
    }

    private String mobileOs() {
        return properties.getApi().getMobileOs();
    }

    private String mobileApp() {
        return properties.getApi().getMobileApp();
    }

    @FunctionalInterface
    private interface ApiSupplier<T> {
        T get();
    }

    @HttpExchange
    private interface TourismHttpExchangeClient {

        @GetExchange("/AreaTarDemDsService/areaTarExpDsList")
        AreaTarExpDsResponse fetchExpenditureIntensity(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("baseYm") String baseYm, @RequestParam("areaCd") String areaCd,
                @RequestParam("numOfRows") int numOfRows, @RequestParam("pageNo") int pageNo,
                @RequestParam(value = "signguCd", required = false) String signguCd,
                @RequestParam(value = "tarExpDsIxCd", required = false) String tarExpDsIxCd
        );

        @GetExchange("/AreaTarDemDsService/areaTarSjrnDsList")
        AreaTarSjrnDsResponse fetchStayIntensity(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("baseYm") String baseYm, @RequestParam("areaCd") String areaCd,
                @RequestParam("numOfRows") int numOfRows, @RequestParam("pageNo") int pageNo,
                @RequestParam(value = "signguCd", required = false) String signguCd,
                @RequestParam(value = "tarSjrnDsIxCd", required = false) String tarSjrnDsIxCd
        );

        @GetExchange("/AreaTarResDemService/areaTarSvcDemList")
        AreaTarSvcDemResponse fetchServiceDemand(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("baseYm") String baseYm, @RequestParam("areaCd") String areaCd,
                @RequestParam("numOfRows") int numOfRows, @RequestParam(value = "signguCd", required = false) String signguCd,
                @RequestParam(value = "tarSvcDemIxCd", required = false) String tarSvcDemIxCd
        );

        @GetExchange("/AreaTarResDemService/areaCulResDemList")
        AreaCulResDemResponse fetchCulturalResourceDemand(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("baseYm") String baseYm, @RequestParam("areaCd") String areaCd,
                @RequestParam("numOfRows") int numOfRows, @RequestParam(value = "signguCd", required = false) String signguCd,
                @RequestParam(value = "culResDemIxCd", required = false) String culResDemIxCd
        );

        @GetExchange("/TatsCnctrRateService/tatsCnctrRatedList")
        TatsCnctrRateResponse fetchConcentrationRate(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("areaCd") String areaCd, @RequestParam("signguCd") String signguCd,
                @RequestParam("numOfRows") int numOfRows, @RequestParam(value = "tAtsNm", required = false) String tAtsNm
        );

        @GetExchange("/AreaTarDivService/areaTouDivList")
        AreaTouDivResponse fetchTouristDiversity(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("baseYm") String baseYm, @RequestParam("areaCd") String areaCd,
                @RequestParam("numOfRows") int numOfRows, @RequestParam(value = "signguCd", required = false) String signguCd,
                @RequestParam(value = "touDivIxCd", required = false) String touDivIxCd
        );

        @GetExchange("/AreaTarDivService/areaExpDivList")
        AreaExpDivResponse fetchExpenditureDiversity(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("baseYm") String baseYm, @RequestParam("areaCd") String areaCd,
                @RequestParam("numOfRows") int numOfRows, @RequestParam(value = "signguCd", required = false) String signguCd,
                @RequestParam(value = "expDivIxCd", required = false) String expDivIxCd
        );

        @GetExchange("/KorService2/areaBasedList2")
        AreaBasedResponse fetchAreaBasedContent(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("numOfRows") int numOfRows, @RequestParam("pageNo") int pageNo,
                @RequestParam("arrange") String arrange, @RequestParam("lDongRegnCd") String lDongRegnCd,
                @RequestParam(value = "lDongSignguCd", required = false) String lDongSignguCd
        );

        @GetExchange("/KorService2/searchKeyword2")
        AreaBasedResponse searchContent(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("keyword") String keyword, @RequestParam("pageNo") int pageNo,
                @RequestParam("numOfRows") int numOfRows, @RequestParam("arrange") String arrange,
                @RequestParam("lDongRegnCd") String lDongRegnCd, @RequestParam(value = "contentTypeId", required = false) String contentTypeId,
                @RequestParam(value = "lDongSignguCd", required = false) String lDongSignguCd
        );

        @GetExchange("/KorService2/detailCommon2")
        ContentDetailResponse fetchCommonDetail(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("contentId") String contentId, @RequestParam("pageNo") int pageNo,
                @RequestParam("numOfRows") int numOfRows
        );

        @GetExchange("/KorService2/detailIntro2")
        DetailIntroResponse fetchIntroDetail(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("contentId") String contentId, @RequestParam("pageNo") int pageNo,
                @RequestParam("numOfRows") int numOfRows, @RequestParam("contentTypeId") String contentTypeId
        );

        @GetExchange("/KorService2/detailInfo2")
        DetailInfoResponse fetchRepeatInfo(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("contentId") String contentId, @RequestParam("pageNo") int pageNo,
                @RequestParam("numOfRows") int numOfRows, @RequestParam("contentTypeId") String contentTypeId
        );

        @GetExchange("/KorService2/detailImage2")
        DetailImageResponse fetchImages(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("contentId") String contentId, @RequestParam("pageNo") int pageNo,
                @RequestParam("numOfRows") int numOfRows, @RequestParam(value = "imageYN", required = false) String imageYN
        );

        @GetExchange("/TarRlteTarService1/searchKeyword1")
        RelatedTourismResponse fetchRelatedTourism(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("baseYm") String baseYm, @RequestParam("areaCd") String areaCd,
                @RequestParam("signguCd") String signguCd, @RequestParam("keyword") String keyword,
                @RequestParam("pageNo") int pageNo, @RequestParam("numOfRows") int numOfRows
        );

        @GetExchange("/KorService2/lclsSystmCode2")
        ClassificationSystemResponse fetchClassificationNames(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("pageNo") int pageNo, @RequestParam("numOfRows") int numOfRows,
                @RequestParam("lclsSystmListYn") String lclsSystmListYn,
                @RequestParam(value = "lclsSystm1", required = false) String lclsSystm1,
                @RequestParam(value = "lclsSystm2", required = false) String lclsSystm2,
                @RequestParam(value = "lclsSystm3", required = false) String lclsSystm3
        );

        @GetExchange("/KorService2/ldongCode2")
        SigunguResponse fetchSigunguList(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("pageNo") int pageNo, @RequestParam("numOfRows") int numOfRows,
                @RequestParam("lDongRegnCd") String lDongRegnCd, @RequestParam("lDongListYn") String lDongListYn
        );

        @GetExchange("/PhotoGalleryService1/gallerySearchList1")
        PhotoGalleryResponse searchPhotos(
                @RequestParam("serviceKey") String serviceKey, @RequestParam("MobileOS") String mobileOs,
                @RequestParam("MobileApp") String mobileApp, @RequestParam("_type") String type,
                @RequestParam("keyword") String keyword, @RequestParam("pageNo") int pageNo,
                @RequestParam("numOfRows") int numOfRows, @RequestParam("arrange") String arrange
        );
    }
}
