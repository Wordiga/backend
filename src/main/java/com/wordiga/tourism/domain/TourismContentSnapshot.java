package com.wordiga.tourism.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

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

    private LocalDateTime sourceModifiedAt;

    private LocalDateTime lastSyncedAt;

    public boolean update(ContentSnapshotValues values, LocalDateTime syncedAt) {
        boolean changed = !Objects.equals(contentTypeId, values.contentTypeId())
                || !Objects.equals(title, values.title())
                || !Objects.equals(firstimage, values.firstimage())
                || !Objects.equals(addr1, values.addr1())
                || !Objects.equals(mapx, values.mapx())
                || !Objects.equals(mapy, values.mapy())
                || !Objects.equals(sigunguCode, values.sigunguCode())
                || !Objects.equals(sigunguName, values.sigunguName())
                || !Objects.equals(lclsSystem1Code, values.lclsSystem1Code())
                || !Objects.equals(lclsSystem2Code, values.lclsSystem2Code())
                || !Objects.equals(lclsSystem3Code, values.lclsSystem3Code())
                || !Objects.equals(sourceModifiedAt, values.sourceModifiedAt());

        if (changed) {
            contentTypeId = values.contentTypeId();
            title = values.title();
            firstimage = values.firstimage();
            addr1 = values.addr1();
            mapx = values.mapx();
            mapy = values.mapy();
            sigunguCode = values.sigunguCode();
            sigunguName = values.sigunguName();
            lclsSystem1Code = values.lclsSystem1Code();
            lclsSystem2Code = values.lclsSystem2Code();
            lclsSystem3Code = values.lclsSystem3Code();
            sourceModifiedAt = values.sourceModifiedAt();
            updatedAt = syncedAt;
        }
        lastSyncedAt = syncedAt;
        return changed;
    }

    public void markSynchronized(LocalDateTime syncedAt) {
        lastSyncedAt = syncedAt;
    }

    public String getLclsSystemCode() {
        if (lclsSystem3Code != null) return lclsSystem3Code;
        if (lclsSystem2Code != null) return lclsSystem2Code;
        if (lclsSystem1Code != null) return lclsSystem1Code;
        return contentTypeId;
    }

    public record ContentSnapshotValues(
            String contentTypeId,
            String title,
            String firstimage,
            String addr1,
            BigDecimal mapx,
            BigDecimal mapy,
            String sigunguCode,
            String sigunguName,
            String lclsSystem1Code,
            String lclsSystem2Code,
            String lclsSystem3Code,
            LocalDateTime sourceModifiedAt
    ) {
    }
}
