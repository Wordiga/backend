package com.wordiga.plan;

import com.wordiga.common.domain.BaseTimeEntity;
import com.wordiga.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer participantCount;

    private Long scheduleId;

    private Long estimatedTotalAmount;

    private Long estimatedPerPersonAmount;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanContent> planContents = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanBudgetBreakdown> budgetBreakdowns = new ArrayList<>();

    private Plan(Member member, String title, LocalDate startDate, LocalDate endDate,
                 Integer participantCount) {
        this.member = member;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.participantCount = participantCount;
    }

    public static Plan create(Member member, String title, LocalDate startDate,
                              LocalDate endDate, Integer participantCount) {
        return new Plan(member, title, startDate, endDate, participantCount);
    }

    public void addContent(PlanContent content) {
        this.planContents.add(content);
    }

    public void update(String title, Integer participantCount) {
        if (title != null) this.title = title.strip();
        if (participantCount != null) {
            this.participantCount = participantCount;
            if (estimatedPerPersonAmount != null)
                this.estimatedTotalAmount = estimatedPerPersonAmount * participantCount;
        }
    }

    public void replaceContents(List<PlanContent> contents) {
        planContents.clear();
        planContents.addAll(contents);
    }

    public void applyAiResult(Long scheduleId, Long totalAmount, Long perPersonAmount,
                              Map<String, Long> breakdown) {
        this.scheduleId = scheduleId;
        this.estimatedTotalAmount = totalAmount;
        this.estimatedPerPersonAmount = perPersonAmount;
        budgetBreakdowns.clear();
        if (breakdown != null) breakdown.forEach((category, amount) -> {
            if (category != null && !category.isBlank() && amount != null)
                budgetBreakdowns.add(PlanBudgetBreakdown.create(this, category, amount));
        });
    }
}