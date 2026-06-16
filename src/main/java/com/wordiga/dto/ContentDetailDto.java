package com.wordiga.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentDetailDto {
    private String contentid;
    private String contenttypeid;
    private String title;
    private String createdtime;
    private String modifiedtime;
    private String tel;
    private String telname;
    private String homepage;
    private String firstimage;
    private String firstimage2;
    private String cpyrhtDivCd;
    private String addr1;
    private String addr2;
    private String zipcode;
    private String mapx;
    private String mapy;
    private String mlevel;
    private String overview;
    private String lDongRegnCd;
    private String lDongSignguCd;
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;
}