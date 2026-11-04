package io.github.renanbacheschi.aguiabranca.api;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.github.renanbacheschi.aguiabranca.support.IsolatedMongoDb;
import io.github.renanbacheschi.aguiabranca.support.FakeAiAnalysisConfiguration;
import io.github.renanbacheschi.aguiabranca.user.UserDocument;
import io.github.renanbacheschi.aguiabranca.user.UserRepository;
import io.github.renanbacheschi.aguiabranca.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Exercita HTTP real, autenticação JWT e serviços/repositórios reais contra MongoDB efêmero. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.seed.enabled=false", "app.ai-analysis.enabled=false",
        "app.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
})
@AutoConfigureTestRestTemplate
@Import(FakeAiAnalysisConfiguration.class)
class ApiContractIntegrationTests {
    private static final String PASSWORD = "Teste@123";
    @Autowired TestRestTemplate rest;
    final ObjectMapper mapper = new ObjectMapper();
    @Autowired MongoTemplate mongo;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    private String operator, manager, leader, strategyId, ideaId, projectId, resultId;

    @DynamicPropertySource static void mongoProperties(DynamicPropertyRegistry registry) { IsolatedMongoDb.register(registry); }

    @BeforeEach void prepare() throws Exception {
        mongo.getDb().drop();
        user("Operador", "operator@contract.local", UserRole.OPERATOR);
        user("Gestor", "manager@contract.local", UserRole.MANAGER);
        user("Líder", "leader@contract.local", UserRole.LEADER);
        operator = login("operator@contract.local"); manager = login("manager@contract.local"); leader = login("leader@contract.local");
    }

    @Test void publicStatusAndAuthenticationContracts() throws Exception {
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/status", null, null), "status.json");
        expect(HttpStatus.OK, call(HttpMethod.GET, "/actuator/health", null, null), "health.json");
        expect(HttpStatus.UNAUTHORIZED, call(HttpMethod.GET, "/api/v1/auth/me", null, null), "problem.json");
        ResponseEntity<String> me = call(HttpMethod.GET, "/api/v1/auth/me", operator, null);
        expect(HttpStatus.OK, me, "user.json"); Assertions.assertEquals("OPERATOR", body(me).path("role").asText());
        expect(HttpStatus.UNAUTHORIZED, call(HttpMethod.POST, "/api/v1/auth/login", null, "{\"email\":\"operator@contract.local\",\"password\":\"errada\"}"), "problem.json");
    }

    @Test void strategyOperationsHaveContractsAndPermissionFailure() throws Exception {
        expect(HttpStatus.FORBIDDEN, call(HttpMethod.POST, "/api/v1/strategies", operator, strategyPayload("Sem permissão")), "problem.json");
        ResponseEntity<String> created = call(HttpMethod.POST, "/api/v1/strategies", leader, strategyPayload("Economia de recursos"));
        expect(HttpStatus.CREATED, created, "strategy.json"); strategyId = body(created).path("id").asText(); Assertions.assertEquals("\"1\"", created.getHeaders().getETag());
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/strategies", operator, null), "page.json");
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/strategies/" + strategyId, manager, null), "strategy.json");
        ResponseEntity<String> updated = call(HttpMethod.PUT, "/api/v1/strategies/" + strategyId, leader, strategyPayload("Economia de água"), "\"1\"");
        expect(HttpStatus.OK, updated, "strategy.json"); Assertions.assertEquals("Economia de água", body(updated).path("title").asText());
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/strategies/" + strategyId + "/history", leader, null), "history.json");
        ResponseEntity<String> deleted = call(HttpMethod.DELETE, "/api/v1/strategies/" + strategyId, leader, null, "\"2\"");
        Assertions.assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode()); Assertions.assertTrue(deleted.getBody() == null || deleted.getBody().isEmpty());
    }

    @Test void ideaAndAnalysisOperationsHaveContractsAndValidationFailure() throws Exception {
        createActiveStrategy();
        expect(HttpStatus.BAD_REQUEST, call(HttpMethod.POST, "/api/v1/ideas", operator, "{}"), "problem.json");
        ResponseEntity<String> disposable = call(HttpMethod.POST, "/api/v1/ideas", operator, ideaPayload("Rascunho removível")); expect(HttpStatus.CREATED, disposable, "idea.json");
        ResponseEntity<String> noContent = call(HttpMethod.DELETE, "/api/v1/ideas/" + body(disposable).path("id").asText(), operator, null); Assertions.assertEquals(HttpStatus.NO_CONTENT, noContent.getStatusCode()); Assertions.assertTrue(noContent.getBody() == null || noContent.getBody().isEmpty());
        ResponseEntity<String> created = call(HttpMethod.POST, "/api/v1/ideas", operator, ideaPayload("Reduzir desperdício")); expect(HttpStatus.CREATED, created, "idea.json"); ideaId = body(created).path("id").asText();
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/ideas", operator, null), "page.json");
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/ideas/" + ideaId, operator, null), "idea.json");
        ResponseEntity<String> updated = call(HttpMethod.PUT, "/api/v1/ideas/" + ideaId, operator, ideaPayload("Reuso de materiais")); expect(HttpStatus.OK, updated, "idea.json");
        ResponseEntity<String> submitted = call(HttpMethod.POST, "/api/v1/ideas/" + ideaId + "/submit", operator, null); expect(HttpStatus.OK, submitted, "idea.json"); Assertions.assertEquals("SUBMITTED", body(submitted).path("status").asText());
        ResponseEntity<String> reviewed = call(HttpMethod.PATCH, "/api/v1/ideas/" + ideaId + "/review", manager, reviewPayload()); expect(HttpStatus.OK, reviewed, "idea.json"); Assertions.assertEquals("APPROVED", body(reviewed).path("status").asText());
        expect(HttpStatus.CREATED, call(HttpMethod.POST, "/api/v1/ideas/" + ideaId + "/analysis", manager, null), "analysis.json");
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/ideas/" + ideaId + "/analysis", manager, null), "analysis.json");
        expect(HttpStatus.CONFLICT, call(HttpMethod.DELETE, "/api/v1/ideas/" + ideaId, operator, null), "problem.json");
    }

    @Test void projectsResultsAndReportsHaveContracts() throws Exception {
        approvedIdea();
        ResponseEntity<String> created = call(HttpMethod.POST, "/api/v1/projects", manager, projectPayload()); expect(HttpStatus.CREATED, created, "project.json"); projectId = body(created).path("id").asText();
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/projects", leader, null), "page.json");
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/projects/" + projectId, manager, null), "project.json");
        expect(HttpStatus.OK, call(HttpMethod.PUT, "/api/v1/projects/" + projectId, manager, projectPayload()), "project.json");
        ResponseEntity<String> progress = call(HttpMethod.PATCH, "/api/v1/projects/" + projectId + "/progress", manager, "{\"stage\":\"EXECUTION\",\"status\":\"IN_PROGRESS\",\"progressPercentage\":50,\"actualInvestment\":120}"); expect(HttpStatus.OK, progress, "project.json"); Assertions.assertEquals(50, body(progress).path("progressPercentage").asInt());
        ResponseEntity<String> result = call(HttpMethod.POST, "/api/v1/projects/" + projectId + "/results", manager, resultPayload()); expect(HttpStatus.CREATED, result, "result.json"); resultId = body(result).path("id").asText();
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/projects/" + projectId + "/results", leader, null), "page.json");
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/projects/" + projectId + "/results/" + resultId, manager, null), "result.json");
        expect(HttpStatus.OK, call(HttpMethod.PUT, "/api/v1/projects/" + projectId + "/results/" + resultId, manager, resultPayload()), "result.json");
        ResponseEntity<String> noContent = call(HttpMethod.DELETE, "/api/v1/projects/" + projectId + "/results/" + resultId, manager, null); Assertions.assertEquals(HttpStatus.NO_CONTENT, noContent.getStatusCode()); Assertions.assertTrue(noContent.getBody() == null || noContent.getBody().isEmpty());
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/reports/summary", leader, null), "summary-report.json");
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/reports/strategies/" + strategyId, leader, null), "strategy-report.json");
        expect(HttpStatus.OK, call(HttpMethod.GET, "/api/v1/reports/projects/" + projectId, leader, null), "project-report.json");
        noContent = call(HttpMethod.DELETE, "/api/v1/projects/" + projectId, manager, null); Assertions.assertEquals(HttpStatus.NO_CONTENT, noContent.getStatusCode()); Assertions.assertTrue(noContent.getBody() == null || noContent.getBody().isEmpty());
    }

    private void approvedIdea() throws Exception { createActiveStrategy(); ResponseEntity<String> idea = call(HttpMethod.POST, "/api/v1/ideas", operator, ideaPayload("Projeto de reuso")); ideaId = body(idea).path("id").asText(); expect(HttpStatus.CREATED, idea, "idea.json"); expect(HttpStatus.OK, call(HttpMethod.POST, "/api/v1/ideas/" + ideaId + "/submit", operator, null), "idea.json"); expect(HttpStatus.OK, call(HttpMethod.PATCH, "/api/v1/ideas/" + ideaId + "/review", manager, reviewPayload()), "idea.json"); }
    private void createActiveStrategy() throws Exception { ResponseEntity<String> strategy = call(HttpMethod.POST, "/api/v1/strategies", leader, strategyPayload("Redução de resíduos")); expect(HttpStatus.CREATED, strategy, "strategy.json"); strategyId = body(strategy).path("id").asText(); }
    private void user(String name, String email, UserRole role) { users.save(new UserDocument(name, email, encoder.encode(PASSWORD), role, true)); }
    private String login(String email) throws Exception { ResponseEntity<String> login = call(HttpMethod.POST, "/api/v1/auth/login", null, "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"); expect(HttpStatus.OK, login, "login.json"); return body(login).path("accessToken").asText(); }
    private ResponseEntity<String> call(HttpMethod method, String path, String token, String payload) { return call(method, path, token, payload, null); }
    private ResponseEntity<String> call(HttpMethod method, String path, String token, String payload, String etag) { HttpHeaders h = new HttpHeaders(); h.setAccept(List.of(MediaType.APPLICATION_JSON)); if (payload != null) h.setContentType(MediaType.APPLICATION_JSON); if (token != null) h.setBearerAuth(token); if (etag != null) h.setIfMatch(etag); return rest.exchange(path, method, new HttpEntity<>(payload, h), String.class); }
    private String strategyPayload(String title) { LocalDate now = LocalDate.now(); return "{\"title\":\"" + title + "\",\"description\":\"Uso eficiente de recursos\",\"category\":\"ESG\",\"campaign\":\"Ciclo sustentável\",\"startsOn\":\"" + now.minusDays(1) + "\",\"endsOn\":\"" + now.plusDays(30) + "\"}"; }
    private String ideaPayload(String title) { return "{\"title\":\"" + title + "\",\"problem\":\"Perdas evitáveis\",\"proposedSolution\":\"Reaproveitar materiais\",\"expectedBenefits\":\"Menos resíduos\",\"strategyId\":\"" + strategyId + "\"}"; }
    private String reviewPayload() { return "{\"decision\":\"APPROVED\",\"priority\":\"HIGH\",\"justification\":\"Viável e mensurável.\"}"; }
    private String projectPayload() { LocalDate now = LocalDate.now(); return "{\"ideaId\":\"" + ideaId + "\",\"name\":\"Reuso de materiais\",\"description\":\"Implantar coleta seletiva\",\"responsibleName\":\"Gestor ESG\",\"plannedStartDate\":\"" + now + "\",\"plannedEndDate\":\"" + now.plusDays(30) + "\",\"plannedInvestment\":300}"; }
    private String resultPayload() { LocalDate now = LocalDate.now(); return "{\"type\":\"COST_SAVING\",\"nature\":\"ACTUAL\",\"description\":\"Economia por reuso\",\"periodStart\":\"" + now.minusDays(7) + "\",\"periodEnd\":\"" + now + "\",\"financialAmount\":150}"; }
    private JsonNode body(ResponseEntity<String> response) throws Exception { return mapper.readTree(response.getBody()); }
    private void expect(HttpStatus status, ResponseEntity<String> response, String schema) throws Exception { Assertions.assertEquals(status, response.getStatusCode(), response.getBody()); validate(schema, body(response)); }
    private void validate(String schemaName, JsonNode document) { InputStream input = getClass().getResourceAsStream("/schemas/" + schemaName); Assertions.assertNotNull(input, schemaName); JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(input); Set<ValidationMessage> errors = schema.validate(document); Assertions.assertTrue(errors.isEmpty(), () -> schemaName + ": " + errors); }
}
