package com.wordiga.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelatedTourismItem {
    private String rlteTatsCd;
    private String rlteTatsNm;
    private String rlteRegnCd;
    private String rlteSignguCd;
    private String rlteCtgryLclsNm;
    private Integer rlteRank;
}
