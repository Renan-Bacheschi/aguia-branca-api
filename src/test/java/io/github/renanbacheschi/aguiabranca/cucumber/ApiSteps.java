package io.github.renanbacheschi.aguiabranca.cucumber;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.github.renanbacheschi.aguiabranca.user.UserDocument;
import io.github.renanbacheschi.aguiabranca.user.UserRepository;
import io.github.renanbacheschi.aguiabranca.user.UserRole;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ApiSteps {
    private static final String PASSWORD = "Teste@123";
    @Autowired private TestRestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired private MongoTemplate mongo;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    private String operatorToken, managerToken, leaderToken, strategyId, ideaId;
    private ResponseEntity<String> response;

    @Before public void resetDatabase() { mongo.getDb().drop(); operatorToken = managerToken = leaderToken = strategyId = ideaId = null; response = null; }

    @Dado("contas reais de operador, gestor e líder") public void realAccounts() throws Exception {
        createUser("Operador ESG", "operator@test.local", UserRole.OPERATOR);
        createUser("Gestor ESG", "manager@test.local", UserRole.MANAGER);
        createUser("Líder ESG", "leader@test.local", UserRole.LEADER);
        operatorToken = login("operator@test.local"); managerToken = login("manager@test.local"); leaderToken = login("leader@test.local");
    }
    @Dado("uma estratégia ativa de redução de desperdício criada pelo líder") public void activeStrategy() throws Exception {
        response = exchange(HttpMethod.POST, "/api/v1/strategies", leaderToken, strategyPayload()); assertStatus(HttpStatus.CREATED);
        strategyId = json().path("id").asText(); assertSchema("strategy.json");
    }
    @Dado("uma ideia de desperdício enviada pelo operador") public void submittedIdea() throws Exception {
        activeStrategy(); response = exchange(HttpMethod.POST, "/api/v1/ideas", operatorToken, ideaPayload()); assertStatus(HttpStatus.CREATED);
        ideaId = json().path("id").asText(); response = exchange(HttpMethod.POST, "/api/v1/ideas/" + ideaId + "/submit", operatorToken, null);
        assertStatus(HttpStatus.OK); Assertions.assertEquals("SUBMITTED", json().path("status").asText()); assertSchema("idea.json");
    }
    @Quando("o operador cadastra a ideia de reaproveitamento de insumos") public void createIdea() throws Exception {
        response = exchange(HttpMethod.POST, "/api/v1/ideas", operatorToken, ideaPayload()); ideaId = json().path("id").asText();
    }
    @Quando("o gestor aprova a ideia com prioridade alta e justificativa") public void approveIdea() { response = exchange(HttpMethod.PATCH, "/api/v1/ideas/" + ideaId + "/review", managerToken, reviewPayload()); }
    @Quando("o operador tenta aprovar a própria ideia") public void denyOwnReview() { response = exchange(HttpMethod.PATCH, "/api/v1/ideas/" + ideaId + "/review", operatorToken, reviewPayload()); }
    @Entao("a ideia é criada com HTTP {int}, estratégia vinculada e contrato válido") public void createdIdea(int status) throws Exception {
        assertStatus(HttpStatus.valueOf(status)); Assertions.assertEquals(strategyId, json().path("strategyId").asText());
        Assertions.assertEquals("Reduzir desperdício de insumos", json().path("title").asText()); Assertions.assertEquals("DRAFT", json().path("status").asText()); assertSchema("idea.json");
    }
    @Entao("a ideia fica aprovada com prioridade {string} e contrato válido") public void approvedIdea(String priority) throws Exception {
        assertStatus(HttpStatus.OK); Assertions.assertEquals("APPROVED", json().path("status").asText()); Assertions.assertEquals(priority, json().path("priority").asText());
        Assertions.assertFalse(json().path("reviewedByUserId").asText().isBlank()); assertSchema("idea.json");
    }
    @Entao("recebe acesso negado HTTP {int} com contrato de erro válido") public void denied(int status) throws Exception { assertStatus(HttpStatus.valueOf(status)); Assertions.assertEquals("Acesso negado", json().path("title").asText()); assertSchema("problem.json"); }
    @Entao("a ideia continua enviada ao consultá-la") public void statePreserved() throws Exception {
        response = exchange(HttpMethod.GET, "/api/v1/ideas/" + ideaId, operatorToken, null); assertStatus(HttpStatus.OK);
        Assertions.assertEquals("SUBMITTED", json().path("status").asText()); assertSchema("idea.json");
    }
    private void createUser(String name, String email, UserRole role) { users.save(new UserDocument(name, email, passwordEncoder.encode(PASSWORD), role, true)); }
    private String login(String email) throws Exception { ResponseEntity<String> login = exchange(HttpMethod.POST, "/api/v1/auth/login", null, "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"); Assertions.assertEquals(HttpStatus.OK, login.getStatusCode()); return mapper.readTree(login.getBody()).path("accessToken").asText(); }
    private ResponseEntity<String> exchange(HttpMethod method, String path, String token, String body) { HttpHeaders h = new HttpHeaders(); h.setAccept(List.of(MediaType.APPLICATION_JSON)); if (body != null) h.setContentType(MediaType.APPLICATION_JSON); if (token != null) h.setBearerAuth(token); return rest.exchange(path, method, new HttpEntity<>(body, h), String.class); }
    private String strategyPayload() { LocalDate now = LocalDate.now(); return "{\"title\":\"Redução de desperdício\",\"description\":\"Evitar perdas operacionais\",\"category\":\"Operações\",\"campaign\":\"ESG 2026\",\"startsOn\":\"" + now.minusDays(1) + "\",\"endsOn\":\"" + now.plusDays(30) + "\"}"; }
    private String ideaPayload() { return "{\"title\":\"Reduzir desperdício de insumos\",\"problem\":\"Descarte de materiais\",\"proposedSolution\":\"Medir e reaproveitar sobras\",\"expectedBenefits\":\"Menos resíduos e custos\",\"strategyId\":\"" + strategyId + "\"}"; }
    private String reviewPayload() { return "{\"decision\":\"APPROVED\",\"priority\":\"HIGH\",\"justification\":\"Impacto mensurável na redução de descarte.\"}"; }
    private JsonNode json() throws Exception { return mapper.readTree(response.getBody()); }
    private void assertStatus(HttpStatus expected) { Assertions.assertEquals(expected, response.getStatusCode(), response.getBody()); }
    private void assertSchema(String name) throws Exception { JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(getClass().getResourceAsStream("/schemas/" + name)); Set<ValidationMessage> errors = schema.validate(json()); Assertions.assertTrue(errors.isEmpty(), () -> "Contrato " + name + " inválido: " + errors); }
}
