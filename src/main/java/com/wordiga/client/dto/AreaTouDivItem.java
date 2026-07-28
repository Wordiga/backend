package com.wordiga.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AreaTouDivItem {

    private String baseYm;
    private String areaCd;
    private String areaNm;
    private String signguCd;
    private String signguNm;
    private String touDivIxCd;
    private String touDivIxNm;
    private String touDivIxVal;
}
