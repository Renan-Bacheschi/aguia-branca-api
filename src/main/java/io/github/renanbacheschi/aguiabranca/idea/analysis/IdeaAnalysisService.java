package io.github.renanbacheschi.aguiabranca.idea.analysis;

import java.time.Clock;

import org.springframework.stereotype.Service;

import io.github.renanbacheschi.aguiabranca.error.ConflictException;
import io.github.renanbacheschi.aguiabranca.error.AnalysisServiceUnavailableException;
import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;
import io.github.renanbacheschi.aguiabranca.idea.IdeaDocument;
import io.github.renanbacheschi.aguiabranca.idea.IdeaRepository;
import io.github.renanbacheschi.aguiabranca.idea.IdeaStatus;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyDocument;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyRepository;

@Service
public class IdeaAnalysisService {

    private final IdeaRepository ideaRepository;
    private final StrategyRepository strategyRepository;
    private final IdeaAnalysisRepository analysisRepository;
    private final IdeaAnalysisProvider analysisProvider;
    private final Clock clock;

    public IdeaAnalysisService(IdeaRepository ideaRepository, StrategyRepository strategyRepository,
            IdeaAnalysisRepository analysisRepository, IdeaAnalysisProvider analysisProvider, Clock clock) {
        this.ideaRepository = ideaRepository;
        this.strategyRepository = strategyRepository;
        this.analysisRepository = analysisRepository;
        this.analysisProvider = analysisProvider;
        this.clock = clock;
    }

    public IdeaAnalysisResponse create(String ideaId, String userId) {
        IdeaDocument idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResourceNotFoundException("Ideia não encontrada."));
        requireEligibleStatus(idea);
        StrategyDocument strategy = strategyRepository.findById(idea.getStrategyId())
                .orElseThrow(() -> new ResourceNotFoundException("Estratégia não encontrada."));
        ProviderIdeaAnalysis providerAnalysis = analysisProvider.analyze(new IdeaAnalysisInput(
                idea.getTitle(), idea.getProblem(), idea.getProposedSolution(), idea.getExpectedBenefits(),
                strategy.getTitle(), strategy.getDescription(), strategy.getCategory(), strategy.getCampaign()));
        validateProviderAnalysis(providerAnalysis);
        IdeaAnalysisDocument analysis = IdeaAnalysisDocument.create(
                idea.getId(), strategy.getId(), providerAnalysis, userId, clock.instant());
        return IdeaAnalysisResponse.from(analysisRepository.save(analysis));
    }

    public IdeaAnalysisResponse getLatest(String ideaId) {
        IdeaDocument idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResourceNotFoundException("Ideia não encontrada."));
        requireEligibleStatus(idea);
        return analysisRepository.findFirstByIdeaIdOrderByGeneratedAtDesc(ideaId)
                .map(IdeaAnalysisResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Parecer de IA não encontrado."));
    }

    private void requireEligibleStatus(IdeaDocument idea) {
        if (idea.getStatus() == IdeaStatus.DRAFT) {
            throw new ConflictException("Ideias em rascunho não podem ser analisadas.");
        }
    }

    private void validateProviderAnalysis(ProviderIdeaAnalysis analysis) {
        if (analysis == null
                || isInvalid(analysis.summary())
                || isInvalid(analysis.strategicAlignment())
                || isInvalid(analysis.potentialBenefits())
                || isInvalid(analysis.risks())
                || isInvalid(analysis.missingInformation())
                || isInvalid(analysis.recommendation())
                || isInvalid(analysis.provider())
                || isInvalid(analysis.model())) {
            throw new AnalysisServiceUnavailableException("O provedor retornou um parecer inválido.");
        }
    }

    private boolean isInvalid(String value) {
        return value == null || value.isBlank() || value.length() > 2_000;
    }
}
