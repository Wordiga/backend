package com.wordiga.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "cart_contents")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false, length = 50)
    private String contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "content_type_id", length = 20)
    private String contentTypeId;

    @Column(length = 500)
    private String thumbnail;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 10)
    private String zipcode;

    @CreatedDate
    @Column(name = "inserted_at", nullable = false, updatable = false)
    private LocalDateTime insertedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private CartContent(String contentId, Member member, String contentTypeId,
                        String thumbnail, String title, String zipcode) {
        this.contentId = contentId;
        this.member = member;
        this.contentTypeId = contentTypeId;
        this.thumbnail = thumbnail;
        this.title = title;
        this.zipcode = zipcode;
    }

    public static CartContent create(String contentId, Member member, String contentTypeId,
                                     String thumbnail, String title, String zipcode) {
        return new CartContent(contentId, member, contentTypeId, thumbnail, title, zipcode);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}