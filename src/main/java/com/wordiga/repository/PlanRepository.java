package com.wordiga.repository;

import com.wordiga.plan.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Page<Plan> findByMemberId(Long memberId, Pageable pageable);

    Optional<Plan> findByIdAndMemberId(Long id, Long memberId);

    boolean existsByMemberIdAndTitle(Long memberId, String title);
}
