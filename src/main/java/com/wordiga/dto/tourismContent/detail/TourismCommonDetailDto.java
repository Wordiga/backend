package com.wordiga.dto.tourismContent.detail;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TourismCommonDetailDto {

    private String contentId;
    private String contentTypeId;
    private String title;
    private String createdTime;
    private String modifiedTime;
    private String tel;
    private String telName;
    private String homepage;
    private String firstImage;
    private String firstImage2;
    private String copyrightTypeCode;
    private String addr1;
    private String addr2;
    private String zipcode;
    private BigDecimal mapx;
    private BigDecimal mapy;
    private String mapLevel;
    private String overview;
    private String lDongRegnCd;
    private String lDongSignguCd;
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;
}
