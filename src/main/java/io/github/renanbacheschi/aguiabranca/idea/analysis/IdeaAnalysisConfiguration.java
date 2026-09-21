package io.github.renanbacheschi.aguiabranca.idea.analysis;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import io.github.renanbacheschi.aguiabranca.error.AnalysisServiceUnavailableException;

@Configuration
@EnableConfigurationProperties(AiAnalysisProperties.class)
public class IdeaAnalysisConfiguration {

    @Bean
    IdeaAnalysisProvider ideaAnalysisProvider(AiAnalysisProperties properties) {
        if (!properties.enabled()) {
            return input -> {
                throw new AnalysisServiceUnavailableException("A análise de ideias está desabilitada.");
            };
        }
        if (isBlank(properties.apiKey()) || isBlank(properties.model())) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY e OPENAI_MODEL são obrigatórios quando AI_ANALYSIS_ENABLED=true.");
        }
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .timeout(Duration.ofSeconds(15))
                .maxRetries(0)
                .build();
        return new OpenAiIdeaAnalysisProvider(client, properties.model());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
