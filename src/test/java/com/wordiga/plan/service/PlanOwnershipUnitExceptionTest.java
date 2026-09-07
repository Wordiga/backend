package com.wordiga.plan.service;

import com.wordiga.plan.repository.PlanRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanOwnershipUnitExceptionTest {
    @Test
    void rejectsPlanOwnedByAnotherMember() {
        PlanRepository plans = mock(PlanRepository.class);
        when(plans.findByIdAndMemberId(7L, 2L)).thenReturn(Optional.empty());
        PlanService service = new PlanService(plans, null, null, null);

        assertThatThrownBy(() -> service.getPlan(2L, 7L)).hasMessageContaining("404");
        verify(plans).findByIdAndMemberId(7L, 2L);
    }
}
