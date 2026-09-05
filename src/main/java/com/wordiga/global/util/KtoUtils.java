package com.wordiga.global.util;

import com.wordiga.global.client.dto.KtoApiResponse;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

public abstract class KtoUtils {

    private static final DateTimeFormatter KTO_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static <T> List<T> extractItems(KtoApiResponse<T> response) {
        if (response == null || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody().getItems().getItem() == null) {
            return Collections.emptyList();
        }
        return response.getResponse().getBody().getItems().getItem();
    }

    public static Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            String digitsOnly = value.replaceAll("[^0-9-]", "");
            return digitsOnly.isEmpty() ? null : Integer.valueOf(digitsOnly);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal parseBigDecimal(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static LocalDateTime parseKtoDateTime(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return LocalDateTime.parse(value, KTO_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }


    public static double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? Double.NaN : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
