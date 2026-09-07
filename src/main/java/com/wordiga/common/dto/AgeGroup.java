package com.wordiga.common.dto;

public enum AgeGroup {
    TWENTIES("20S"),
    THIRTIES("30S"),
    FORTIES("40S"),
    FIFTIES_PLUS("50S_PLUS");

    private final String code;

    AgeGroup(String code) {
        this.code = code;
    }

    @com.fasterxml.jackson.annotation.JsonValue
    public String code() {
        return code;
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgeGroup from(String code) {
        return java.util.Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 연령대입니다."));
    }
}
