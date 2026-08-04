package com.wordiga.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Plan extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    @Column(name = "plan_type", length = 50)
    private String planType;

    @Column(name = "schedule_id", length = 100)
    private String scheduleId;

    @Lob
    @Column(name = "ai_response_json")
    private String aiResponseJson;

    @Column(name = "estimated_total_amount")
    private Long estimatedTotalAmount;

    @Column(name = "estimated_per_person_amount")
    private Long estimatedPerPersonAmount;

    @Lob
    @Column(name = "estimated_budget_breakdown")
    private String estimatedBudgetBreakdown;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanContent> planContents = new ArrayList<>();

    private Plan(Member member, String title, LocalDate startDate, LocalDate endDate,
                 Integer participantCount, String planType) {
        this.member = member;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.participantCount = participantCount;
        this.planType = planType;
    }

    public static Plan create(Member member, String title, LocalDate startDate,
                              LocalDate endDate, Integer participantCount, String planType) {
        return new Plan(member, title, startDate, endDate, participantCount, planType);
    }

    public void addContent(PlanContent content) {
        this.planContents.add(content);
    }

    public void update(String title, Integer participantCount) {
        if (title != null) this.title = title.strip();
        if (participantCount != null) {
            this.participantCount = participantCount;
            if (estimatedPerPersonAmount != null) this.estimatedTotalAmount = estimatedPerPersonAmount * participantCount;
        }
    }

    public void replaceContents(List<PlanContent> contents) {
        planContents.clear();
        planContents.addAll(contents);
    }

    public void applyAiResult(String scheduleId, String aiResponseJson, Long totalAmount,
                              Long perPersonAmount, String breakdown) {
        this.scheduleId = scheduleId;
        this.aiResponseJson = aiResponseJson;
        this.estimatedTotalAmount = totalAmount;
        this.estimatedPerPersonAmount = perPersonAmount;
        this.estimatedBudgetBreakdown = breakdown;
    }
}
