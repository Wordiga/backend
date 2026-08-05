package com.wordiga.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan_budget_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "category"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanBudgetItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;
    @Column(nullable = false, length = 50)
    private String category;
    @Column(nullable = false)
    private Long amount;

    private PlanBudgetItem(Plan plan, String category, Long amount) {
        this.plan = plan;
        this.category = category;
        this.amount = amount;
    }

    public static PlanBudgetItem create(Plan plan, String category, Long amount) {
        return new PlanBudgetItem(plan, category, amount);
    }
}
