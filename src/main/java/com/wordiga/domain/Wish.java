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

    @Column(nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private TourismContentSnapshot content;

    @Column(nullable = false, length = 50)
    private String folderName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Wish create(Long memberId, TourismContentSnapshot content, String folderName) {
        return Wish.builder()
                .memberId(memberId)
                .content(content)
                .folderName(folderName != null ? folderName : "기본 위시리스트")
                .build();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public String getContentId() {
        return this.content.getContentId();
    }

    public String getSigunguCode() {
        return this.content.getSigunguCode();
    }

    public String getLclsSystemCode() {
        return this.content.getLclsSystemCode();
    }
}