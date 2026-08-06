package com.wordiga.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassificationSystemItem {
    private String lclsSystm1Cd;
    private String lclsSystm1Nm;
    private String lclsSystm2Cd;
    private String lclsSystm2Nm;
    private String lclsSystm3Cd;
    private String lclsSystm3Nm;
}
