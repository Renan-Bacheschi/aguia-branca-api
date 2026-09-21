package io.github.renanbacheschi.aguiabranca.auth;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.renanbacheschi.aguiabranca.user.UserDocument;
import io.github.renanbacheschi.aguiabranca.user.UserRepository;
import io.github.renanbacheschi.aguiabranca.user.UserRole;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.health.mongodb.enabled=false",
        "spring.data.mongodb.auto-index-creation=false",
        "app.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
})
@AutoConfigureMockMvc
@Import(AuthenticationIntegrationTests.SecuredTestConfiguration.class)
class AuthenticationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void loginReturnsTokenAndUserWithoutPasswordHash() throws Exception {
        UserDocument user = activeUser(UserRole.OPERATOR);
        when(userRepository.findByEmail("operator@demo.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "  OPERATOR@DEMO.COM  ",
                                  "password": "Operator@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(7200))
                .andExpect(jsonPath("$.user.id").value("user-1"))
                .andExpect(jsonPath("$.user.name").value("Demo Operator"))
                .andExpect(jsonPath("$.user.email").value("operator@demo.com"))
                .andExpect(jsonPath("$.user.role").value("OPERATOR"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginRejectsIncorrectPassword() throws Exception {
        UserDocument user = activeUser(UserRole.OPERATOR);
        when(userRepository.findByEmail("operator@demo.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"operator@demo.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void loginRejectsInactiveUser() throws Exception {
        UserDocument user = user(false, UserRole.OPERATOR);
        when(userRepository.findByEmail("operator@demo.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"operator@demo.com","password":"Operator@123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void loginRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.email").value("O e-mail deve ser válido."))
                .andExpect(jsonPath("$.errors.password").value("A senha é obrigatória."));
    }

    @Test
    void currentUserReturnsAuthenticatedUser() throws Exception {
        UserDocument user = activeUser(UserRole.OPERATOR);
        when(userRepository.findByEmail("operator@demo.com")).thenReturn(Optional.of(user));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"operator@demo.com","password":"Operator@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.accessToken");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.email").value("operator@demo.com"))
                .andExpect(jsonPath("$.role").value("OPERATOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void currentUserWithoutTokenReturnsUnauthorizedProblem() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidTokenReturnsUnauthorizedProblem() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void methodSecurityReturnsForbiddenProblem() throws Exception {
        UserDocument user = activeUser(UserRole.OPERATOR);
        when(userRepository.findByEmail("operator@demo.com")).thenReturn(Optional.of(user));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"operator@demo.com","password":"Operator@123"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.accessToken");

        mockMvc.perform(get("/test/leader").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    private UserDocument activeUser(UserRole role) {
        return user(true, role);
    }

    private UserDocument user(boolean active, UserRole role) {
        return new UserDocument(
                "user-1",
                "Demo Operator",
                "operator@demo.com",
                passwordEncoder.encode("Operator@123"),
                role,
                active,
                Instant.parse("2026-09-20T12:00:00Z"),
                Instant.parse("2026-09-20T12:00:00Z"));
    }

    @TestConfiguration
    static class SecuredTestConfiguration {

        @RestController
        static class SecuredTestController {

            @GetMapping("/test/leader")
            @PreAuthorize("hasRole('LEADER')")
            String leaderOnly() {
                return "ok";
            }
        }
    }
}
