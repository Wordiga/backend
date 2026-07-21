package com.wordiga.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AreaTarSvcDemItem {
    private String baseYm;
    private String areaCd;
    private String areaNm;
    private String signguCd;
    private String signguNm;
    private String tarSvcDemIxCd;
    private String tarSvcDemIxNm;
    private String tarSvcDemIxVal;
}