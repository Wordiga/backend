package com.wordiga.domain;

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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "plan_id", nullable = false) private Plan plan;
    @Column(name = "s3_key", nullable = false, unique = true, length = 500) private String s3Key;
    @Column(name = "file_name", nullable = false, length = 255) private String fileName;
    @Column(name = "file_size", nullable = false) private long fileSize;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;

    private Proposal(Plan plan, String s3Key, String fileName, long fileSize, LocalDateTime expiresAt) {
        this.plan = plan; this.s3Key = s3Key; this.fileName = fileName; this.fileSize = fileSize; this.expiresAt = expiresAt;
    }
    public static Proposal create(Plan plan, String s3Key, String fileName, long fileSize, LocalDateTime expiresAt) {
        return new Proposal(plan, s3Key, fileName, fileSize, expiresAt);
    }
}
