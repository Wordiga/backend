package com.wordiga.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "plan_contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "content_title", nullable = false, length = 255)
    private String contentTitle;

    @Column(name = "content_type_id", length = 20)
    private String contentTypeId;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column
    private Integer duration;

    private PlanContent(Plan plan, Integer sequence, String contentTitle,
                        String contentTypeId, LocalDateTime scheduledTime, Integer duration) {
        this.plan = plan;
        this.sequence = sequence;
        this.contentTitle = contentTitle;
        this.contentTypeId = contentTypeId;
        this.scheduledTime = scheduledTime;
        this.duration = duration;
    }

    public static PlanContent create(Plan plan, Integer sequence, String contentTitle,
                                     String contentTypeId, LocalDateTime scheduledTime, Integer duration) {
        return new PlanContent(plan, sequence, contentTitle, contentTypeId, scheduledTime, duration);
    }
}