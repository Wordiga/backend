package com.wordiga.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "content_types")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentType {

    @Id
    @Column(name = "content_type_id", length = 20)
    private String contentTypeId;

    @Column(name = "lcls_system1_code", length = 20)
    private String lclsSystem1Code;

    @Column(name = "lcls_system1_name", length = 100)
    private String lclsSystem1Name;

    @Column(name = "lcls_system2_code", length = 20)
    private String lclsSystem2Code;

    @Column(name = "lcls_system2_name", length = 100)
    private String lclsSystem2Name;

    @Column(name = "lcls_system3_code", length = 20)
    private String lclsSystem3Code;

    @Column(name = "lcls_system3_name", length = 100)
    private String lclsSystem3Name;

    private ContentType(String contentTypeId, String lclsSystem1Code, String lclsSystem1Name,
                        String lclsSystem2Code, String lclsSystem2Name,
                        String lclsSystem3Code, String lclsSystem3Name) {
        this.contentTypeId = contentTypeId;
        this.lclsSystem1Code = lclsSystem1Code;
        this.lclsSystem1Name = lclsSystem1Name;
        this.lclsSystem2Code = lclsSystem2Code;
        this.lclsSystem2Name = lclsSystem2Name;
        this.lclsSystem3Code = lclsSystem3Code;
        this.lclsSystem3Name = lclsSystem3Name;
    }

    public static ContentType of(String contentTypeId, String sys1Code, String sys1Name,
                                 String sys2Code, String sys2Name,
                                 String sys3Code, String sys3Name) {
        return new ContentType(contentTypeId, sys1Code, sys1Name, sys2Code, sys2Name, sys3Code, sys3Name);
    }
}