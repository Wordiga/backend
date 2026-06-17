package com.wordiga.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LclsSystmCodeDto {
    private Integer rnum;
    private String code;
    private String name;
    private String lclsSystm1Cd;
    private String lclsSystm1Nm;
    private String lclsSystm2Cd;
    private String lclsSystm2Nm;
    private String lclsSystm3Cd;
    private String lclsSystm3Nm;
}