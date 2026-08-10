package com.wordiga.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SigunguItem {
    @JsonProperty("IDongSignguCd")
    private String code;

    @JsonProperty("IDongSignguNm")
    private String name;

    private String rnum;
}