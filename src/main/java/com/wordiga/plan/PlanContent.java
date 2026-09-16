package com.wordiga.plan;

import com.wordiga.tourism.domain.TourismContentSnapshot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @JoinColumn(nullable = false)
    private Plan plan;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false)
    private Integer dayNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private TourismContentSnapshot content;

    private LocalDateTime scheduledTime;

    private Integer duration;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer travelTimeMinutes;

    private Integer travelDistanceMeters;

    private Long estimatedCost;

    private Long unitAmount;

    @Enumerated(EnumType.STRING)
    private CostUnit costUnit;

    private Integer costQuantity;

    private Long perPersonShare;

    @Enumerated(EnumType.STRING)
    private CostSource costSource;

    private PlanContent(Plan plan, Integer sequence, Integer dayNumber, TourismContentSnapshot content) {
        this.plan = plan;
        this.sequence = sequence;
        this.dayNumber = dayNumber;
        this.content = content;
    }

    public static PlanContent create(Plan plan, Integer sequence, Integer dayNumber, TourismContentSnapshot content) {
        return new PlanContent(plan, sequence, dayNumber, content);
    }

    public void updateAiDetails(LocalDateTime scheduledTime, Integer duration, LocalTime startTime,
                                LocalTime endTime, Integer travelTimeMinutes, Integer travelDistanceMeters, Long estimatedCost) {
        this.scheduledTime = scheduledTime;
        this.duration = duration;
        this.startTime = startTime;
        this.endTime = endTime;
        this.travelTimeMinutes = travelTimeMinutes;
        this.travelDistanceMeters = travelDistanceMeters;
        this.estimatedCost = estimatedCost;
    }

    public void updateCost(Long unitAmount, CostUnit unit, Integer quantity, Long totalAmount,
                           Long perPersonShare, CostSource source) {
        this.unitAmount = unitAmount;
        this.costUnit = unit;
        this.costQuantity = quantity;
        this.estimatedCost = totalAmount;
        this.perPersonShare = perPersonShare;
        this.costSource = source;
    }

    public void copyDetailsFrom(PlanContent source) {
        updateAiDetails(source.scheduledTime, source.duration, source.startTime, source.endTime,
                source.travelTimeMinutes, source.travelDistanceMeters, source.estimatedCost);
        updateCost(source.unitAmount, source.costUnit, source.costQuantity, source.estimatedCost,
                source.perPersonShare, source.costSource);
    }
}
