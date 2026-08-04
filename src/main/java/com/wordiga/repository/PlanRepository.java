package com.wordiga.repository;

import com.wordiga.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Page<Plan> findByMemberId(Long memberId, Pageable pageable);
    Optional<Plan> findByIdAndMemberId(Long id, Long memberId);
}
