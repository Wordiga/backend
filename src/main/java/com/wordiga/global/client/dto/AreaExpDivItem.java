package com.wordiga.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AreaExpDivItem {

    private String baseYm;
    private String areaCd;
    private String areaNm;
    private String signguCd;
    private String signguNm;
    private String expDivIxCd;
    private String expDivIxNm;
    private String expDivIxVal;
}
