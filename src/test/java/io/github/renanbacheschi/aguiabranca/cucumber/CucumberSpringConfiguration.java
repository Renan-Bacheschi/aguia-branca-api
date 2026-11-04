package io.github.renanbacheschi.aguiabranca.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import io.github.renanbacheschi.aguiabranca.support.IsolatedMongoDb;
import io.github.renanbacheschi.aguiabranca.support.FakeAiAnalysisConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.seed.enabled=false",
        "app.ai-analysis.enabled=false",
        "app.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
})
@AutoConfigureTestRestTemplate
@Import(FakeAiAnalysisConfiguration.class)
public class CucumberSpringConfiguration {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        IsolatedMongoDb.register(registry);
    }
}
