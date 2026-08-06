package com.wordiga.client;

import com.wordiga.client.dto.AsosDailyResponse;
import com.wordiga.global.config.WeatherProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WeatherApiClient {
    private final RestClient restClient;
    private final WeatherProperties properties;

    public List<AsosDailyResponse.Item> daily(String stationId, LocalDate start, LocalDate end) {
        configured();
        URI uri = URI.create(properties.baseUrl() + "/getWthrDataList?serviceKey=" + properties.serviceKey()
                + "&pageNo=1&numOfRows=100&dataType=JSON&dataCd=ASOS&dateCd=DAY&stnIds=" + stationId
                + "&startDt=" + start.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "&endDt=" + end.format(DateTimeFormatter.BASIC_ISO_DATE));
        try {
            AsosDailyResponse response = restClient.get().uri(uri).retrieve().body(AsosDailyResponse.class);
            if (response == null || response.getResponse() == null) unavailable();
            var header = response.getResponse().getHeader();
            if (header != null && header.getResultCode() != null && !"00".equals(header.getResultCode())) unavailable();
            var body = response.getResponse().getBody();
            return body == null || body.getItems() == null || body.getItems().getItem() == null
                    ? List.of() : body.getItems().getItem();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "기상청 API를 사용할 수 없습니다.", exception);
        }
    }

    private void configured() {
        if (properties.serviceKey() == null || properties.serviceKey().isBlank()) unavailable();
    }

    private void unavailable() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "기상청 API를 사용할 수 없습니다.");
    }
}
