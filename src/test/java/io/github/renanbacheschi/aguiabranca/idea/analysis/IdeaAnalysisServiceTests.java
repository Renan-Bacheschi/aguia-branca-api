package io.github.renanbacheschi.aguiabranca.idea.analysis;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.renanbacheschi.aguiabranca.error.AnalysisServiceUnavailableException;
import io.github.renanbacheschi.aguiabranca.error.ConflictException;
import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;
import io.github.renanbacheschi.aguiabranca.idea.IdeaDocument;
import io.github.renanbacheschi.aguiabranca.idea.IdeaRepository;
import io.github.renanbacheschi.aguiabranca.idea.IdeaStatus;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyDocument;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdeaAnalysisServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-21T15:00:00Z");

    @Mock
    private IdeaRepository ideaRepository;
    @Mock
    private StrategyRepository strategyRepository;
    @Mock
    private IdeaAnalysisRepository analysisRepository;
    @Mock
    private IdeaAnalysisProvider analysisProvider;

    private IdeaAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new IdeaAnalysisService(
                ideaRepository, strategyRepository, analysisRepository, analysisProvider,
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(analysisRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void managerAnalysisPersistsIdeaStrategyUserAndDateWithoutChangingIdea() {
        IdeaDocument idea = idea(IdeaStatus.SUBMITTED);
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea));
        when(strategyRepository.findById("strategy-1")).thenReturn(Optional.of(strategy()));
        when(analysisProvider.analyze(any())).thenReturn(validAnalysis());

        IdeaAnalysisResponse response = service.create("idea-1", "manager-1");

        ArgumentCaptor<IdeaAnalysisDocument> captor = ArgumentCaptor.forClass(IdeaAnalysisDocument.class);
        verify(analysisRepository).save(captor.capture());
        IdeaAnalysisDocument persisted = captor.getValue();
        assertThat(persisted.getIdeaId()).isEqualTo("idea-1");
        assertThat(persisted.getStrategyId()).isEqualTo("strategy-1");
        assertThat(persisted.getGeneratedByUserId()).isEqualTo("manager-1");
        assertThat(persisted.getGeneratedAt()).isEqualTo(NOW);
        assertThat(response.provider()).isEqualTo("openai");
        assertThat(idea.getStatus()).isEqualTo(IdeaStatus.SUBMITTED);
        assertThat(idea.getPriority()).isNull();
        assertThat(idea.getReviewedByUserId()).isNull();
    }

    @Test
    void approvedAndRejectedIdeasCanBeAnalyzed() {
        for (IdeaStatus status : java.util.List.of(IdeaStatus.APPROVED, IdeaStatus.REJECTED)) {
            when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea(status)));
            when(strategyRepository.findById("strategy-1")).thenReturn(Optional.of(strategy()));
            when(analysisProvider.analyze(any())).thenReturn(validAnalysis());

            assertThat(service.create("idea-1", "manager-1").ideaId()).isEqualTo("idea-1");
        }
    }

    @Test
    void draftIdeaCannotBeAnalyzed() {
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea(IdeaStatus.DRAFT)));

        assertThatThrownBy(() -> service.create("idea-1", "manager-1"))
                .isInstanceOf(ConflictException.class);
        verify(analysisProvider, never()).analyze(any());
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void missingIdeaOrStrategyIsReported() {
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create("idea-1", "manager-1"))
                .isInstanceOf(ResourceNotFoundException.class);

        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea(IdeaStatus.SUBMITTED)));
        when(strategyRepository.findById("strategy-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create("idea-1", "manager-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void providerFailureOrInvalidResponseDoesNotPersistAnalysis() {
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea(IdeaStatus.SUBMITTED)));
        when(strategyRepository.findById("strategy-1")).thenReturn(Optional.of(strategy()));
        when(analysisProvider.analyze(any()))
                .thenThrow(new AnalysisServiceUnavailableException("Indisponível"))
                .thenReturn(new ProviderIdeaAnalysis(null, "A", "B", "C", "D", "E", "openai", "model"));

        assertThatThrownBy(() -> service.create("idea-1", "manager-1"))
                .isInstanceOf(AnalysisServiceUnavailableException.class);
        assertThatThrownBy(() -> service.create("idea-1", "manager-1"))
                .isInstanceOf(AnalysisServiceUnavailableException.class);
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void latestAnalysisIsReturnedAndOlderAnalysisRemainsUntouched() {
        IdeaAnalysisDocument older = IdeaAnalysisDocument.create(
                "idea-1", "strategy-1", validAnalysis(), "manager-1", NOW.minusSeconds(60));
        IdeaAnalysisDocument latest = IdeaAnalysisDocument.create(
                "idea-1", "strategy-1", validAnalysis(), "manager-2", NOW);
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea(IdeaStatus.SUBMITTED)));
        when(analysisRepository.findFirstByIdeaIdOrderByGeneratedAtDesc("idea-1")).thenReturn(Optional.of(latest));

        IdeaAnalysisResponse response = service.getLatest("idea-1");

        assertThat(response.generatedByUserId()).isEqualTo("manager-2");
        assertThat(older.getGeneratedAt()).isBefore(response.generatedAt());
    }

    @Test
    void newAnalysisCreatesAnotherDocumentWithoutDeletingThePreviousOne() {
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea(IdeaStatus.SUBMITTED)));
        when(strategyRepository.findById("strategy-1")).thenReturn(Optional.of(strategy()));
        when(analysisProvider.analyze(any())).thenReturn(validAnalysis());

        service.create("idea-1", "manager-1");
        service.create("idea-1", "manager-1");

        ArgumentCaptor<IdeaAnalysisDocument> captor = ArgumentCaptor.forClass(IdeaAnalysisDocument.class);
        verify(analysisRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(1)).isNotSameAs(captor.getAllValues().get(0));
        verify(analysisRepository, never()).delete(any());
    }

    @Test
    void latestAnalysisRequiresExistingIdeaAndAnalysis() {
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLatest("idea-1")).isInstanceOf(ResourceNotFoundException.class);

        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea(IdeaStatus.SUBMITTED)));
        when(analysisRepository.findFirstByIdeaIdOrderByGeneratedAtDesc("idea-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLatest("idea-1")).isInstanceOf(ResourceNotFoundException.class);
    }

    private IdeaDocument idea(IdeaStatus status) {
        IdeaDocument idea = IdeaDocument.create(
                "Title", "Problem", "Solution", "Benefits", "strategy-1", "operator-1", NOW);
        ReflectionTestUtils.setField(idea, "id", "idea-1");
        if (status == IdeaStatus.SUBMITTED) {
            idea.submit(NOW);
        } else if (status != IdeaStatus.DRAFT) {
            idea.submit(NOW);
            idea.review(status, null, "Review", "manager-1", NOW);
        }
        return idea;
    }

    private StrategyDocument strategy() {
        StrategyDocument strategy = StrategyDocument.create(
                "Strategy", "Description", "Category", "Campaign", NOW.atZone(ZoneOffset.UTC).toLocalDate(),
                NOW.atZone(ZoneOffset.UTC).toLocalDate().plusDays(1), "leader-1", NOW);
        ReflectionTestUtils.setField(strategy, "id", "strategy-1");
        return strategy;
    }

    private ProviderIdeaAnalysis validAnalysis() {
        return new ProviderIdeaAnalysis("Summary", "Alignment", "Benefits", "Risks", "Missing", "Recommendation",
                "openai", "gpt-test");
    }
}
