package com.wordiga.tourism.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tourism_content_snapshots")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TourismContentSnapshot {

    @Id
    @Column(length = 50, nullable = false)
    private String contentId;

    @Column(length = 20)
    private String contentTypeId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String firstimage;

    @Column(length = 255)
    private String addr1;

    @Column(precision = 15, scale = 10)
    private BigDecimal mapx;

    @Column(precision = 15, scale = 10)
    private BigDecimal mapy;

    @Column(length = 10)
    private String sigunguCode;

    @Column(length = 50)
    private String sigunguName;

    @Column(length = 20)
    private String lclsSystem1Code;

    @Column(length = 20)
    private String lclsSystem2Code;

    @Column(length = 20)
    private String lclsSystem3Code;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public String getLclsSystemCode() {
        if (lclsSystem3Code != null) return lclsSystem3Code;
        if (lclsSystem2Code != null) return lclsSystem2Code;
        if (lclsSystem1Code != null) return lclsSystem1Code;
        return contentTypeId;
    }
}