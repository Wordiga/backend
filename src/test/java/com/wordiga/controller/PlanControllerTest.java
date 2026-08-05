package com.wordiga.controller;

import com.wordiga.dto.plan.*;
import com.wordiga.security.JwtTokenProvider;
import com.wordiga.service.PlanService;
import com.wordiga.service.PlanGenerationService;
import com.wordiga.service.ProposalService;
import com.wordiga.dto.proposal.ProposalResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlanControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PlanService planService;
    @MockitoBean PlanGenerationService planGenerationService;
    @MockitoBean ProposalService proposalService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @BeforeEach void login() { SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("1", null, List.of())); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void handlesPlanCrudEndpoints() throws Exception {
        when(planService.getPlans(1L, 0, 20, PlanSort.LATEST)).thenReturn(
                PlanListResponse.builder().items(List.of()).page(0).size(20).hasNext(false).build());
        when(planService.getPlan(1L, 9L)).thenReturn(detail());
        when(planService.updatePlan(eq(1L), eq(9L), any())).thenReturn(detail());
        when(planService.updateContents(eq(1L), eq(9L), any())).thenReturn(detail());

        mockMvc.perform(get("/api/v1/plans")).andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0));
        mockMvc.perform(get("/api/v1/plans/9")).andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(9));
        mockMvc.perform(patch("/api/v1/plans/9").contentType("application/json")
                .content("{\"title\":\"수정 일정\",\"participantCount\":4}" )).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/plans/9/contents").contentType("application/json")
                .content("{\"days\":[{\"dayNumber\":1,\"date\":\"2026-08-20\",\"contentIds\":[\"126508\"]}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/plans/9")).andExpect(status().isNoContent());
        verify(planService).deletePlan(1L, 9L);
    }

    @Test void handlesGenerationAndProposalEndpoints() throws Exception {
        when(planGenerationService.generate(eq(1L), any())).thenReturn(detail());
        when(proposalService.create(eq(1L), eq(9L), any())).thenReturn(
                ProposalResponse.builder().proposalId(3L).planId(9L).fileName("proposal.docx").build());
        when(proposalService.list(1L, 9L)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/plans/generate").contentType("application/json").content("""
                {"startDate":"2026-08-20","endDate":"2026-08-21","participantCount":2,
                 "selectedContentIds":["126508"]}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(9));
        mockMvc.perform(post("/api/v1/plans/9/proposals").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fileName").value("proposal.docx"));
        mockMvc.perform(get("/api/v1/plans/9/proposals")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/plans/9/proposals/3")).andExpect(status().isNoContent());
        verify(proposalService).delete(1L, 9L, 3L);
    }

    private PlanDetailResponse detail() { return PlanDetailResponse.builder().planId(9L).title("일정")
            .days(List.of()).build(); }
}
