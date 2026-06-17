package com.wordiga.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailInfoDto {
    private String contentid;
    private String contenttypeid;

    // 일반 반복정보
    private String fldgubun;
    private String infoname;
    private String infotext;
    private String serialnum;

    // 여행코스 반복정보
    private String subcontentid;
    private String subdetailalt;
    private String subdetailimg;
    private String subdetailoverview;
    private String subname;
    private String subnum;

    // 숙박 객실정보
    private String roomcode;
    private String roomtitle;
    private String roomsize1;
    private String roomcount;
    private String roombasecount;
    private String roommaxcount;
    private String roomoffseasonminfee1;
    private String roomoffseasonminfee2;
    private String roompeakseasonminfee1;
    private String roompeakseasonminfee2;
    private String roomintro;
    private String roombathfacility;
    private String roombath;
    private String roomhometheater;
    private String roomaircondition;
    private String roomtv;
    private String roompc;
    private String roomcable;
    private String roominternet;
    private String roomrefrigerator;
    private String roomtoiletries;
    private String roomsofa;
    private String roomcook;
    private String roomtable;
    private String roomhairdryer;
    private String roomsize2;
    private String roomimg1;
    private String roomimg1alt;
    private String cpyrhtDivCd1;
    private String roomimg2;
    private String roomimg2alt;
    private String cpyrhtDivCd2;
    private String roomimg3;
    private String roomimg3alt;
    private String cpyrhtDivCd3;
    private String roomimg4;
    private String roomimg4alt;
    private String cpyrhtDivCd4;
    private String roomimg5;
    private String roomimg5alt;
    private String cpyrhtDivCd5;
}