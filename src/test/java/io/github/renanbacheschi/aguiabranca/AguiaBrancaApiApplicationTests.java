package io.github.renanbacheschi.aguiabranca;

import java.time.Instant;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.health.mongodb.enabled=false",
        "spring.data.mongodb.auto-index-creation=false",
        "app.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
})
@AutoConfigureMockMvc
class AguiaBrancaApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void statusIsPublicAndReturnsApplicationStatusAndCurrentTimestamp() throws Exception {
        Instant beforeRequest = Instant.now();

        String response = mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.application").value("aguia-branca-api"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andReturn().getResponse().getContentAsString();

        String timestamp = JsonPath.read(response, "$.timestamp");
        assertThat(timestamp).endsWith("Z");
        assertThat(Instant.parse(timestamp)).isBetween(beforeRequest, Instant.now());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void infoIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/private", "/api/v1/status/details", "/actuator/health/readiness",
            "/actuator/info/details", "/actuator", "/actuator/env", "/login", "/"
    })
    void otherPathsRequireAuthentication(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/env", "/actuator/beans", "/actuator/metrics"})
    @WithMockUser
    void otherActuatorEndpointsAreNotExposed(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }

}
