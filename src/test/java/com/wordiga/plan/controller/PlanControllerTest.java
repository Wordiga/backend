package com.wordiga.plan.controller;

import com.wordiga.plan.dto.PlanDetailResponse;
import com.wordiga.plan.dto.PlanListResponse;
import com.wordiga.plan.dto.PlanSort;
import com.wordiga.proposal.dto.ProposalResponse;
import com.wordiga.global.security.JwtTokenProvider;
import com.wordiga.member.repository.MemberRepository;
import com.wordiga.plan.service.PlanGenerationService;
import com.wordiga.plan.service.PlanService;
import com.wordiga.proposal.service.ProposalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlanControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    PlanService planService;
    @MockitoBean
    PlanGenerationService planGenerationService;
    @MockitoBean
    ProposalService proposalService;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    MemberRepository memberRepository;

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handlesPlanCrudEndpoints() throws Exception {
        when(planService.getPlans(1L, 0, 20, PlanSort.LATEST)).thenReturn(
                PlanListResponse.builder().items(List.of()).page(0).size(20).totalCount(3).hasNext(false).build());
        when(planService.getPlan(1L, 9L)).thenReturn(detail());
        when(planService.updatePlan(eq(1L), eq(9L), any())).thenReturn(detail());
        when(planService.updateContents(eq(1L), eq(9L), any())).thenReturn(detail());

        mockMvc.perform(get("/api/v1/plans")).andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.totalCount").value(3));
        mockMvc.perform(get("/api/v1/plans/9")).andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(9));
        mockMvc.perform(patch("/api/v1/plans/9").contentType("application/json")
                .content("{\"title\":\"수정 일정\",\"participantCount\":4}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/plans/9/contents").contentType("application/json")
                        .content("{\"days\":[{\"dayNumber\":1,\"date\":\"2026-08-20\",\"contentIds\":[\"126508\"]}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/plans/9")).andExpect(status().isNoContent());
        verify(planService).deletePlan(1L, 9L);
    }

    @Test
    void handlesGenerationAndProposalEndpoints() throws Exception {
        when(planGenerationService.generate(eq(1L), any())).thenReturn(detail());
        when(proposalService.create(1L, 9L)).thenReturn(
                ProposalResponse.builder().proposalId(3L).planId(9L).fileName("proposal.docx").build());

        mockMvc.perform(post("/api/v1/plans/generate").contentType("application/json").content("""
                {"visitMonth":"2026-08","stayDays":2,"participantCount":10,
                 "selectedContentIds":["126508"]}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(9));
        mockMvc.perform(post("/api/v1/plans/9/proposals"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fileName").value("proposal.docx"));
        mockMvc.perform(get("/api/v1/plans/9/proposals")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/plans/9/proposals/3")).andExpect(status().isNoContent());
        verify(proposalService).delete(1L, 9L, 3L);
    }

    private PlanDetailResponse detail() {
        return PlanDetailResponse.builder().planId(9L).title("일정")
                .days(List.of()).build();
    }
}
