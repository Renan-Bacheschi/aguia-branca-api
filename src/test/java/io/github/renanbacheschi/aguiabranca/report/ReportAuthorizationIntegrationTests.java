package io.github.renanbacheschi.aguiabranca.report;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.health.mongodb.enabled=false",
        "spring.data.mongodb.auto-index-creation=false",
        "app.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
})
@AutoConfigureMockMvc
class ReportAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    void leaderCanAccessAllReports() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary").with(user("LEADER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/strategies/strategy-1").with(user("LEADER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/projects/project-1").with(user("LEADER")))
                .andExpect(status().isOk());
    }

    @Test
    void managerAndOperatorReceiveForbidden() throws Exception {
        for (String role : java.util.List.of("MANAGER", "OPERATOR")) {
            mockMvc.perform(get("/api/v1/reports/summary").with(user(role)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/reports/strategies/strategy-1").with(user(role)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/reports/projects/project-1").with(user(role)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void requestWithoutTokenReceivesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reports/strategies/strategy-1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reports/projects/project-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingStrategyAndProjectReceiveNotFound() throws Exception {
        when(reportService.getStrategyReport("missing"))
                .thenThrow(new ResourceNotFoundException("Estratégia não encontrada."));
        when(reportService.getProjectReport("missing"))
                .thenThrow(new ResourceNotFoundException("Projeto não encontrado."));

        mockMvc.perform(get("/api/v1/reports/strategies/missing").with(user("LEADER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/reports/projects/missing").with(user("LEADER")))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user(String role) {
        return jwt().jwt(builder -> builder.claim("userId", role.toLowerCase() + "-1").claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
