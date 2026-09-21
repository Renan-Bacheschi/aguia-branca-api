package io.github.renanbacheschi.aguiabranca.idea.analysis;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.renanbacheschi.aguiabranca.error.AnalysisServiceUnavailableException;
import io.github.renanbacheschi.aguiabranca.error.ConflictException;
import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.health.mongodb.enabled=false",
        "spring.data.mongodb.auto-index-creation=false",
        "app.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
})
@AutoConfigureMockMvc
class IdeaAnalysisAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdeaAnalysisService ideaAnalysisService;

    private IdeaAnalysisResponse analysis;

    @BeforeEach
    void setUp() {
        analysis = new IdeaAnalysisResponse(
                "idea-1", "strategy-1", "Summary", "Alignment", "Benefits", "Risks", "Missing",
                "Recommendation", Instant.parse("2026-09-21T15:00:00Z"), "manager-1", "openai", "gpt-test");
        when(ideaAnalysisService.create("idea-1", "manager-1")).thenReturn(analysis);
        when(ideaAnalysisService.getLatest("idea-1")).thenReturn(analysis);
    }

    @Test
    void managerCanCreateAndReadAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/ideas/idea-1/analysis").with(user("MANAGER", "manager-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ideaId").value("idea-1"))
                .andExpect(jsonPath("$.generatedByUserId").value("manager-1"));
        mockMvc.perform(get("/api/v1/ideas/idea-1/analysis").with(user("MANAGER", "manager-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategyId").value("strategy-1"));
    }

    @Test
    void operatorAndLeaderCannotAccessAnalysis() throws Exception {
        for (String role : java.util.List.of("OPERATOR", "LEADER")) {
            mockMvc.perform(post("/api/v1/ideas/idea-1/analysis").with(user(role, role.toLowerCase())))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void analysisRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/ideas/idea-1/analysis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void serviceErrorsUseExpectedProblemDetails() throws Exception {
        when(ideaAnalysisService.create(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("Ideia não encontrada."))
                .thenThrow(new ConflictException("Ideias em rascunho não podem ser analisadas."))
                .thenThrow(new AnalysisServiceUnavailableException("A análise de ideias está desabilitada."));

        mockMvc.perform(post("/api/v1/ideas/missing/analysis").with(user("MANAGER", "manager-1"))
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/ideas/draft/analysis").with(user("MANAGER", "manager-1"))
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/ideas/disabled/analysis").with(user("MANAGER", "manager-1"))
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Serviço de análise indisponível"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(String role, String userId) {
        return jwt().jwt(builder -> builder.claim("userId", userId).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
