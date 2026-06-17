package com.wordiga.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FestivalListDto {
    private String addr1;
    private String addr2;
    private String zipcode;
    private String contentid;
    private String contenttypeid;
    private String createdtime;
    private String eventstartdate;
    private String eventenddate;
    private String firstimage;
    private String firstimage2;
    private String cpyrhtDivCd;
    private String mapx;
    private String mapy;
    private String mlevel;
    private String modifiedtime;
    private String tel;
    private String title;
    private String lDongRegnCd;
    private String lDongSignguCd;
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;
    private String progresstype;
    private String festivaltype;
}