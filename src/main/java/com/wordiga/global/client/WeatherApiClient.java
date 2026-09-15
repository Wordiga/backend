package com.wordiga.global.client;

import com.wordiga.global.client.dto.AwsDailyResponse;
import com.wordiga.global.config.WeatherProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

@Component
public class WeatherApiClient {

    private final AsosHttpExchangeClient client;
    private final WeatherProperties properties;

    public WeatherApiClient(RestClient.Builder restClientBuilder, WeatherProperties properties) {
        this.properties = properties;

        RestClient restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        this.client = factory.createClient(AsosHttpExchangeClient.class);
    }

    public List<AwsDailyResponse.DailyObservation> daily(String stationId, int year, int month) {
        validateServiceKey();

        try {
            AwsDailyResponse response = client.fetchDailyWeather(
                    properties.serviceKey(),
                    1,
                    100,
                    "JSON",
                    year,
                    String.format("%02d", month),
                    stationId
            );

            return extractItems(response);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "기상청 API 호출 중 오류가 발생했습니다.", e);
        }
    }

    private void validateServiceKey() {
        if (!StringUtils.hasText(properties.serviceKey())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "기상청 API 서비스 키가 설정되지 않았습니다.");
        }
    }

    private List<AwsDailyResponse.DailyObservation> extractItems(AwsDailyResponse response) {
        if (response == null || response.getResponse() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "기상청 API 응답이 올바르지 않습니다.");
        }

        var header = response.getResponse().getHeader();
        if (header != null && !"00".equals(header.getResultCode())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "기상청 API 에러 코드: " + header.getResultCode());
        }

        var body = response.getResponse().getBody();
        if (body == null || body.getItems() == null || body.getItems().getItem() == null) {
            return List.of();
        }
        return body.getItems().getItem().stream()
                .filter(item -> item.getAwsmdays() != null && item.getAwsmdays().getInfo() != null)
                .flatMap(item -> item.getAwsmdays().getInfo().stream())
                .toList();
    }

    @HttpExchange
    private interface AsosHttpExchangeClient {
        @GetExchange("/getDailyAwsData")
        AwsDailyResponse fetchDailyWeather(
                @RequestParam("authKey") String serviceKey,
                @RequestParam("pageNo") int pageNo,
                @RequestParam("numOfRows") int numOfRows,
                @RequestParam("dataType") String dataType,
                @RequestParam("year") int year,
                @RequestParam("month") String month,
                @RequestParam("station") String stationId
        );
    }
}
