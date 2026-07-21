package com.wordiga.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "content_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Wish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "content_id", nullable = false, length = 50)
    private String contentId;

    @Column(name = "content_type_id", length = 20)
    private String contentTypeId;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String firstimage;

    @Column(length = 255)
    private String addr1;

    @Column(name = "sigungu_code", length = 10)
    private String sigunguCode;

    @Column(name = "sigungu_name", length = 50)
    private String sigunguName;

    @Column(name = "folder_name", nullable = false, length = 50)
    private String folderName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Wish create(Long memberId, String contentId, String contentTypeId,
                              String title, String firstimage, String addr1,
                              String sigunguCode, String sigunguName, String folderName) {
        return Wish.builder()
                .memberId(memberId)
                .contentId(contentId)
                .contentTypeId(contentTypeId)
                .title(title)
                .firstimage(firstimage)
                .addr1(addr1)
                .sigunguCode(sigunguCode)
                .sigunguName(sigunguName)
                .folderName(folderName != null ? folderName : "기본 위시리스트")
                .build();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}