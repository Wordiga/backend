package com.wordiga.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AreaBasedItem {
    private String contentid;
    private String contenttypeid;
    private String title;
    private String addr1;
    private String addr2;
    private String firstimage;
    private String firstimage2;
    private String cpyrhtDivCd;
    private String mapx;
    private String mapy;
    private String mlevel;
    private String modifiedtime;
    private String createdtime;
    private String tel;
    private String zipcode;

    @JsonProperty("lDongRegnCd")
    private String lDongRegnCd;
    @JsonProperty("lDongSignguCd")
    private String lDongSignguCd;
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;
}
