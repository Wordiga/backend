package com.wordiga.proposal;

import com.wordiga.common.domain.BaseTimeEntity;
import com.wordiga.plan.Plan;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "proposals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private Plan plan;

    @Column(nullable = false, unique = true, length = 500)
    private String s3Key;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private long fileSize;

    @Column(length = 500)
    private String s3KeyPdf;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private Proposal(Plan plan, String s3Key, String s3KeyPdf, String fileName, long fileSize, LocalDateTime expiresAt) {
        this.plan = plan;
        this.s3Key = s3Key;
        this.s3KeyPdf = s3KeyPdf;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.expiresAt = expiresAt;
    }

    public static Proposal create(Plan plan, String s3Key, String s3KeyPdf, String fileName, long fileSize, LocalDateTime expiresAt) {
        return new Proposal(plan, s3Key, s3KeyPdf, fileName, fileSize, expiresAt);
    }
}
