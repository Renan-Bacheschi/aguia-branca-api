package io.github.renanbacheschi.aguiabranca;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.renanbacheschi.aguiabranca.idea.IdeaService;
import io.github.renanbacheschi.aguiabranca.project.ProjectResponse;
import io.github.renanbacheschi.aguiabranca.project.ProjectResultService;
import io.github.renanbacheschi.aguiabranca.project.ProjectService;
import io.github.renanbacheschi.aguiabranca.project.ProjectStage;
import io.github.renanbacheschi.aguiabranca.project.ProjectStatus;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyResponse;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.health.mongodb.enabled=false",
        "spring.data.mongodb.auto-index-creation=false",
        "app.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
})
@AutoConfigureMockMvc
class DomainAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StrategyService strategyService;
    @MockitoBean
    private IdeaService ideaService;
    @MockitoBean
    private ProjectService projectService;
    @MockitoBean
    private ProjectResultService projectResultService;

    @BeforeEach
    void setUp() {
        StrategyResponse strategy = new StrategyResponse(
                "strategy-1", "Title", "Description", "Category", "Campaign",
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"),
                true, false, 1, "leader-1", Instant.parse("2026-09-20T12:00:00Z"),
                Instant.parse("2026-09-20T12:00:00Z"));
        when(strategyService.create(any(), anyString())).thenReturn(strategy);
        when(strategyService.get("strategy-1")).thenReturn(strategy);
        when(strategyService.update(anyString(), org.mockito.ArgumentMatchers.anyLong(), any(), anyString()))
                .thenReturn(strategy);

        ProjectResponse project = new ProjectResponse(
                "project-1", "idea-1", "strategy-1", "Project", "Description", "Responsible",
                ProjectStage.PLANNING, ProjectStatus.PLANNED, LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-31"), null, BigDecimal.TEN, BigDecimal.ZERO, 0,
                false, "manager-1", null, Instant.parse("2026-09-20T12:00:00Z"),
                Instant.parse("2026-09-20T12:00:00Z"));
        when(projectService.create(any(), anyString())).thenReturn(project);
        when(projectService.get("project-1")).thenReturn(project);
    }

    @Test
    void onlyLeaderCreatesStrategies() throws Exception {
        String body = """
                {"title":"Title","description":"Description","category":"Category","campaign":"Campaign",
                 "startsOn":"2026-09-01","endsOn":"2026-09-30"}
                """;

        mockMvc.perform(post("/api/v1/strategies").with(user("LEADER", "leader-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/strategies").with(user("MANAGER", "manager-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void allThreeProfilesReadStrategies() throws Exception {
        for (String role : java.util.List.of("OPERATOR", "MANAGER", "LEADER")) {
            mockMvc.perform(get("/api/v1/strategies/strategy-1").with(user(role, role.toLowerCase())))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void onlyLeaderUpdatesAndArchivesStrategies() throws Exception {
        String body = """
                {"title":"Title","description":"Description","category":"Category","campaign":"Campaign",
                 "startsOn":"2026-09-01","endsOn":"2026-09-30"}
                """;

        mockMvc.perform(put("/api/v1/strategies/strategy-1").with(user("LEADER", "leader-1"))
                        .header("If-Match", "\"1\"").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/strategies/strategy-1").with(user("MANAGER", "manager-1"))
                        .header("If-Match", "\"1\"").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/strategies/strategy-1").with(user("LEADER", "leader-1"))
                        .header("If-Match", "\"1\""))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/strategies/strategy-1").with(user("OPERATOR", "operator-1"))
                        .header("If-Match", "\"1\""))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCreatesIdeasAndOnlyManagerReviews() throws Exception {
        String createBody = """
                {"title":"Idea","problem":"Problem","proposedSolution":"Solution",
                 "expectedBenefits":"Benefits","strategyId":"strategy-1"}
                """;
        String reviewBody = """
                {"decision":"APPROVED","priority":"HIGH","justification":"Approved"}
                """;

        mockMvc.perform(post("/api/v1/ideas").with(user("OPERATOR", "operator-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/ideas").with(user("MANAGER", "manager-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/ideas/idea-1/review").with(user("MANAGER", "manager-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(reviewBody))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/ideas/idea-1/review").with(user("OPERATOR", "operator-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(reviewBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewRequiresPriorityAndJustification() throws Exception {
        mockMvc.perform(patch("/api/v1/ideas/idea-1/review").with(user("MANAGER", "manager-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyManagerCreatesProjectsAndLeaderCanRead() throws Exception {
        String body = """
                {"ideaId":"idea-1","name":"Project","description":"Description",
                 "responsibleName":"Responsible","plannedStartDate":"2026-10-01",
                 "plannedEndDate":"2026-10-31","plannedInvestment":10}
                """;

        mockMvc.perform(post("/api/v1/projects").with(user("MANAGER", "manager-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/projects").with(user("LEADER", "leader-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/project-1").with(user("LEADER", "leader-1")))
                .andExpect(status().isOk());
    }

    @Test
    void operatorCannotAccessProjectsAndAnonymousCannotAccessNewEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/projects/project-1").with(user("OPERATOR", "operator-1")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/strategies/strategy-1"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(String role, String userId) {
        return jwt().jwt(builder -> builder.claim("userId", userId).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
