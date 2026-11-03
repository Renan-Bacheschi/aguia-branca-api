package io.github.renanbacheschi.aguiabranca.support;

import io.github.renanbacheschi.aguiabranca.idea.analysis.IdeaAnalysisProvider;
import io.github.renanbacheschi.aguiabranca.idea.analysis.ProviderIdeaAnalysis;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Substitui somente a integração externa de IA; os serviços de negócio continuam reais. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeAiAnalysisConfiguration {
    @Bean
    @Primary
    IdeaAnalysisProvider localTestIdeaAnalysisProvider() {
        return input -> new ProviderIdeaAnalysis(
                "Resumo local para " + input.title(), "Alinhada à estratégia " + input.strategyTitle(),
                "Redução de perdas", "Acompanhar indicadores", "Nenhuma", "Prosseguir com piloto",
                "local-test", "sem-chave");
    }
}
