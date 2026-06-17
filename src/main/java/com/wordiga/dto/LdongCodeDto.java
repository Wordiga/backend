package com.wordiga.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdongCodeDto {
    private Integer rnum;
    private String code;
    private String name;
    private String lDongRegnCd;
    private String lDongRegnNm;
    private String lDongSignguCd;
    private String lDongSignguNm;
}