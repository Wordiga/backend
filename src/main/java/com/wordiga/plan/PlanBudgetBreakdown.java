package com.wordiga.plan;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan_budget_breakdowns",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "category"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanBudgetBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Long amount;

    private PlanBudgetBreakdown(Plan plan, String category, Long amount) {
        this.plan = plan;
        this.category = category;
        this.amount = amount;
    }

    public static PlanBudgetBreakdown create(Plan plan, String category, Long amount) {
        return new PlanBudgetBreakdown(plan, category, amount);
    }

}
