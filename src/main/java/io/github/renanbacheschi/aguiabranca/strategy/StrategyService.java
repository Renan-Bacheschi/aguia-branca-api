package io.github.renanbacheschi.aguiabranca.strategy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import io.github.renanbacheschi.aguiabranca.common.PageResponse;
import io.github.renanbacheschi.aguiabranca.error.ConflictException;
import io.github.renanbacheschi.aguiabranca.error.InvalidRequestException;
import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;

@Service
public class StrategyService {

    private final StrategyRepository strategyRepository;
    private final Clock clock;

    public StrategyService(StrategyRepository strategyRepository, Clock clock) {
        this.strategyRepository = strategyRepository;
        this.clock = clock;
    }

    public StrategyResponse create(StrategyCreateRequest request, String userId) {
        validatePeriod(request.startsOn(), request.endsOn());
        Instant now = clock.instant();
        StrategyDocument strategy = StrategyDocument.create(
                request.title().trim(), request.description().trim(), request.category().trim(),
                request.campaign().trim(), request.startsOn(), request.endsOn(), userId, now);
        return toResponse(strategyRepository.save(strategy));
    }

    public PageResponse<StrategyResponse> list(Boolean active, int page, int size) {
        validatePage(page, size);
        LocalDate today = LocalDate.now(clock);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<StrategyDocument> strategies;
        if (active == null) {
            strategies = strategyRepository.findByArchivedFalse(pageable);
        } else if (active) {
            strategies = strategyRepository.findByArchivedFalseAndStartsOnLessThanEqualAndEndsOnGreaterThanEqual(
                    today, today, pageable);
        } else {
            strategies = strategyRepository.findInactiveOn(today, pageable);
        }
        return PageResponse.from(strategies, this::toResponse);
    }

    public StrategyResponse get(String id) {
        return toResponse(getDocument(id));
    }

    public StrategyResponse update(String id, long expectedRevision, StrategyUpdateRequest request, String userId) {
        validatePeriod(request.startsOn(), request.endsOn());
        StrategyDocument strategy = getDocument(id);
        requireRevision(strategy, expectedRevision);
        if (strategy.isArchived()) {
            throw new ConflictException("Uma estratégia arquivada não pode ser alterada.");
        }
        strategy.update(
                request.title().trim(), request.description().trim(), request.category().trim(),
                request.campaign().trim(), request.startsOn(), request.endsOn(), userId, clock.instant());
        return toResponse(strategyRepository.save(strategy));
    }

    public void archive(String id, long expectedRevision, String userId) {
        StrategyDocument strategy = getDocument(id);
        requireRevision(strategy, expectedRevision);
        if (strategy.isArchived()) {
            throw new ConflictException("A estratégia já está arquivada.");
        }
        strategy.archive(userId, clock.instant());
        strategyRepository.save(strategy);
    }

    public List<StrategyHistoryResponse> history(String id) {
        return getDocument(id).getHistory().stream()
                .map(StrategyHistoryResponse::from)
                .toList();
    }

    public StrategyDocument requireActive(String id) {
        StrategyDocument strategy = getDocument(id);
        if (!strategy.isActiveOn(LocalDate.now(clock))) {
            throw new ConflictException("A estratégia deve estar vigente e não arquivada.");
        }
        return strategy;
    }

    public StrategyDocument getDocument(String id) {
        return strategyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estratégia não encontrada."));
    }

    private StrategyResponse toResponse(StrategyDocument strategy) {
        return StrategyResponse.from(strategy, LocalDate.now(clock));
    }

    private void requireRevision(StrategyDocument strategy, long expectedRevision) {
        if (strategy.getRevision() != expectedRevision) {
            throw new ConflictException("A estratégia foi alterada por outra requisição.");
        }
    }

    private void validatePeriod(LocalDate startsOn, LocalDate endsOn) {
        if (endsOn.isBefore(startsOn)) {
            throw new InvalidRequestException("A data final não pode ser anterior à data inicial.");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("A página deve ser não negativa e o tamanho deve estar entre 1 e 100.");
        }
    }
}
