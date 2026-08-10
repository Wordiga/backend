package com.wordiga.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailImageDto {
    private String contentid;
    private String imgname;
    private String originimgurl;
    private String serialnum;
    private String cpyrhtDivCd;
    private String smallimageurl;
}