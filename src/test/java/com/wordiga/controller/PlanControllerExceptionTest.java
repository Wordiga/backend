package com.wordiga.controller;

import com.wordiga.security.JwtTokenProvider;
import com.wordiga.service.PlanGenerationService;
import com.wordiga.service.PlanService;
import com.wordiga.service.ProposalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

@WebMvcTest(PlanController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlanControllerExceptionTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PlanService planService;
    @MockitoBean PlanGenerationService planGenerationService;
    @MockitoBean ProposalService proposalService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;

    @BeforeEach void login() { SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("1", null, List.of())); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void rejectsMissingAndInvalidGenerationBody() throws Exception {
        mockMvc.perform(post("/api/v1/plans/generate").contentType("application/json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(post("/api/v1/plans/generate").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors").isArray());
        verifyNoInteractions(planGenerationService);
    }

    @Test void rejectsInvalidPagingAndSort() throws Exception {
        mockMvc.perform(get("/api/v1/plans").param("size", "51"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/api/v1/plans").param("sort", "UNKNOWN"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(planService);
    }

    @Test void rejectsInvalidProposalInput() throws Exception {
        mockMvc.perform(post("/api/v1/plans/1/proposals").contentType("application/json")
                        .content("{\"proposalTitle\":\"" + "가".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("proposalTitle"));
        verifyNoInteractions(proposalService);
    }
}
