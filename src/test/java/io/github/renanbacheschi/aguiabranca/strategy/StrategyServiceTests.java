package io.github.renanbacheschi.aguiabranca.strategy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import io.github.renanbacheschi.aguiabranca.error.ConflictException;
import io.github.renanbacheschi.aguiabranca.error.InvalidRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StrategyServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");

    @Mock
    private StrategyRepository strategyRepository;

    private StrategyService strategyService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        strategyService = new StrategyService(strategyRepository, clock);
        lenient().when(strategyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creationBuildsFirstImmutableHistoryRevision() {
        StrategyResponse response = strategyService.create(request(), "leader-1");

        ArgumentCaptor<StrategyDocument> captor = ArgumentCaptor.forClass(StrategyDocument.class);
        verify(strategyRepository).save(captor.capture());
        StrategyDocument saved = captor.getValue();
        assertThat(response.revision()).isEqualTo(1);
        assertThat(response.active()).isTrue();
        assertThat(response.createdByUserId()).isEqualTo("leader-1");
        assertThat(saved.getHistory()).hasSize(1);
        assertThat(saved.getHistory().getFirst().operation()).isEqualTo(StrategyHistoryOperation.CREATED);
    }

    @Test
    void invalidPeriodIsRejected() {
        StrategyCreateRequest invalid = new StrategyCreateRequest(
                "Title", "Description", "Category", "Campaign",
                LocalDate.parse("2026-09-21"), LocalDate.parse("2026-09-20"));

        assertThatThrownBy(() -> strategyService.create(invalid, "leader-1"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void activeFilterUsesInjectedClockDate() {
        when(strategyRepository.findByArchivedFalseAndStartsOnLessThanEqualAndEndsOnGreaterThanEqual(
                any(), any(), any())).thenReturn(new PageImpl<>(java.util.List.of()));

        strategyService.list(true, 0, 20);

        verify(strategyRepository).findByArchivedFalseAndStartsOnLessThanEqualAndEndsOnGreaterThanEqual(
                eq(LocalDate.parse("2026-09-20")), eq(LocalDate.parse("2026-09-20")), any());
    }

    @Test
    void updateAndArchivePreserveAllHistoryRevisions() {
        StrategyDocument strategy = existingStrategy();
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));

        strategyService.update(strategy.getId(), 1, new StrategyUpdateRequest(
                "Updated", "Description", "Category", "Campaign",
                LocalDate.parse("2026-09-19"), LocalDate.parse("2026-09-22")), "leader-2");
        strategyService.archive(strategy.getId(), 2, "leader-3");

        assertThat(strategy.getHistory())
                .extracting(StrategyRevisionSnapshot::operation)
                .containsExactly(
                        StrategyHistoryOperation.CREATED,
                        StrategyHistoryOperation.UPDATED,
                        StrategyHistoryOperation.ARCHIVED);
        assertThat(strategy.getHistory()).extracting(StrategyRevisionSnapshot::revision)
                .containsExactly(1L, 2L, 3L);
        assertThat(strategy.isArchived()).isTrue();
    }

    @Test
    void staleRevisionIsRejected() {
        StrategyDocument strategy = existingStrategy();
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));

        assertThatThrownBy(() -> strategyService.update(strategy.getId(), 9,
                new StrategyUpdateRequest("Title", "Description", "Category", "Campaign",
                        LocalDate.parse("2026-09-19"), LocalDate.parse("2026-09-21")), "leader-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alterada");
    }

    private StrategyCreateRequest request() {
        return new StrategyCreateRequest(
                "Title", "Description", "Category", "Campaign",
                LocalDate.parse("2026-09-19"), LocalDate.parse("2026-09-21"));
    }

    private StrategyDocument existingStrategy() {
        return StrategyDocument.create(
                "Title", "Description", "Category", "Campaign",
                LocalDate.parse("2026-09-19"), LocalDate.parse("2026-09-21"), "leader-1", NOW);
    }

}
