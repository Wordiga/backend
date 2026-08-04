package com.wordiga.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "plan_contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PlanContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "plan_date", nullable = false)
    private LocalDate date;

    @Column(name = "content_id", nullable = false, length = 50)
    private String contentId;

    @Column(name = "content_title", nullable = false, length = 255)
    private String contentTitle;

    @Column(name = "content_type_id", length = 20)
    private String contentTypeId;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column
    private Integer duration;

    private String addr1;
    @Column(length = 500) private String thumbnailUrl;
    @Column(precision = 15, scale = 10) private BigDecimal mapx;
    @Column(precision = 15, scale = 10) private BigDecimal mapy;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer travelTimeMinutes;
    private Integer travelDistanceMeters;
    private Long estimatedCost;
    private String memo;

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

    public static PlanContent createForUpdate(Plan plan, int dayNumber, LocalDate date, int sequence,
                                              String contentId, String title, String contentTypeId,
                                              String addr1, String thumbnailUrl, BigDecimal mapx, BigDecimal mapy) {
        PlanContent content = new PlanContent(plan, sequence, title, contentTypeId, null, null);
        content.dayNumber = dayNumber;
        content.date = date;
        content.contentId = contentId;
        content.addr1 = addr1;
        content.thumbnailUrl = thumbnailUrl;
        content.mapx = mapx;
        content.mapy = mapy;
        return content;
    }

    public static PlanContent createFromAi(Plan plan, int dayNumber, LocalDate date, int sequence,
                                            String contentId, String title, String contentTypeId, String addr1,
                                            BigDecimal mapx, BigDecimal mapy, LocalTime startTime, LocalTime endTime,
                                            Integer duration, Integer travelTime, Integer travelDistance,
                                            Long estimatedCost, String memo) {
        PlanContent content = createForUpdate(plan, dayNumber, date, sequence, contentId, title,
                contentTypeId, addr1, null, mapx, mapy);
        content.startTime = startTime;
        content.endTime = endTime;
        content.duration = duration;
        content.travelTimeMinutes = travelTime;
        content.travelDistanceMeters = travelDistance;
        content.estimatedCost = estimatedCost;
        content.memo = memo;
        return content;
    }
}
