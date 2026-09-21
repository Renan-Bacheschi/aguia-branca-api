package io.github.renanbacheschi.aguiabranca.idea.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai-analysis")
public record AiAnalysisProperties(boolean enabled, String apiKey, String model) {
}
