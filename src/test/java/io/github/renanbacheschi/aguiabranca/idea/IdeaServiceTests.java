package io.github.renanbacheschi.aguiabranca.idea;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import io.github.renanbacheschi.aguiabranca.auth.AuthenticatedUserContext;
import io.github.renanbacheschi.aguiabranca.error.ConflictException;
import io.github.renanbacheschi.aguiabranca.error.InvalidRequestException;
import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyService;
import io.github.renanbacheschi.aguiabranca.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdeaServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");

    @Mock
    private IdeaRepository ideaRepository;
    @Mock
    private StrategyService strategyService;
    @Mock
    private MongoTemplate mongoTemplate;

    private IdeaService ideaService;

    @BeforeEach
    void setUp() {
        ideaService = new IdeaService(
                ideaRepository, strategyService, mongoTemplate, Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(ideaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creationUsesAuthenticatedAuthorAndRequiresActiveStrategy() {
        IdeaResponse response = ideaService.create(createRequest(), "operator-1");

        verify(strategyService).requireActive("strategy-1");
        assertThat(response.authorUserId()).isEqualTo("operator-1");
        assertThat(response.status()).isEqualTo(IdeaStatus.DRAFT);
    }

    @Test
    void missingExpiredOrArchivedStrategyPreventsCreation() {
        when(strategyService.requireActive("strategy-1"))
                .thenThrow(new ResourceNotFoundException("Estratégia não encontrada."))
                .thenThrow(new ConflictException("A estratégia está fora da vigência."))
                .thenThrow(new ConflictException("A estratégia está arquivada."));

        assertThatThrownBy(() -> ideaService.create(createRequest(), "operator-1"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> ideaService.create(createRequest(), "operator-1"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> ideaService.create(createRequest(), "operator-1"))
                .isInstanceOf(ConflictException.class);
        verify(ideaRepository, never()).save(any());
    }

    @Test
    void operatorCannotReadAnotherOperatorsIdea() {
        IdeaDocument idea = draft("operator-2");
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea));

        assertThatThrownBy(() -> ideaService.get(
                "idea-1", new AuthenticatedUserContext("operator-1", UserRole.OPERATOR)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void onlyDraftOwnedByOperatorCanBeEditedOrDeleted() {
        IdeaDocument idea = draft("operator-1");
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea));

        IdeaResponse updated = ideaService.update("idea-1", new IdeaUpdateRequest(
                "Updated", "Problem", "Solution", "Benefits", "strategy-1"), "operator-1");
        ideaService.delete("idea-1", "operator-1");

        assertThat(updated.title()).isEqualTo("Updated");
        verify(ideaRepository).delete(idea);
    }

    @Test
    void submittedIdeaCannotBeEditedOrDeletedByOperator() {
        IdeaDocument idea = draft("operator-1");
        idea.submit(NOW);
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea));

        assertThatThrownBy(() -> ideaService.delete("idea-1", "operator-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void submissionIsIdempotentAndKeepsFirstTimestamp() {
        IdeaDocument idea = draft("operator-1");
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea));

        IdeaResponse first = ideaService.submit("idea-1", "operator-1");
        IdeaResponse second = ideaService.submit("idea-1", "operator-1");

        assertThat(first.status()).isEqualTo(IdeaStatus.SUBMITTED);
        assertThat(second.submittedAt()).isEqualTo(first.submittedAt());
        verify(ideaRepository).save(idea);
    }

    @ParameterizedTest
    @EnumSource(value = IdeaStatus.class, names = {"APPROVED", "REJECTED"})
    void managerDecisionRecordsPriorityJustificationAndReviewer(IdeaStatus decision) {
        IdeaDocument idea = draft("operator-1");
        idea.submit(NOW);
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(idea));

        IdeaResponse response = ideaService.review(
                "idea-1", new ReviewRequest(decision, IdeaPriority.HIGH, "Justification"), "manager-1");

        assertThat(response.status()).isEqualTo(decision);
        assertThat(response.priority()).isEqualTo(IdeaPriority.HIGH);
        assertThat(response.reviewJustification()).isEqualTo("Justification");
        assertThat(response.reviewedByUserId()).isEqualTo("manager-1");
        assertThat(response.reviewedAt()).isEqualTo(NOW);
    }

    @Test
    void invalidDecisionAndRepeatedReviewAreRejected() {
        IdeaDocument submitted = draft("operator-1");
        submitted.submit(NOW);
        when(ideaRepository.findById("idea-1")).thenReturn(Optional.of(submitted));

        assertThatThrownBy(() -> ideaService.review(
                "idea-1", new ReviewRequest(IdeaStatus.DRAFT, IdeaPriority.LOW, "Invalid"), "manager-1"))
                .isInstanceOf(InvalidRequestException.class);

        submitted.review(IdeaStatus.APPROVED, IdeaPriority.HIGH, "Approved", "manager-1", NOW);
        assertThatThrownBy(() -> ideaService.review(
                "idea-1", new ReviewRequest(IdeaStatus.REJECTED, IdeaPriority.LOW, "Again"), "manager-1"))
                .isInstanceOf(ConflictException.class);
    }

    private IdeaCreateRequest createRequest() {
        return new IdeaCreateRequest("Title", "Problem", "Solution", "Benefits", "strategy-1");
    }

    private IdeaDocument draft(String authorUserId) {
        return IdeaDocument.create(
                "Title", "Problem", "Solution", "Benefits", "strategy-1", authorUserId, NOW);
    }
}
