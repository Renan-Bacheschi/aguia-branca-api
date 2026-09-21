package io.github.renanbacheschi.aguiabranca.strategy;

import java.time.Instant;
import java.time.LocalDate;

public record StrategyHistoryResponse(
        String strategyId,
        long revision,
        StrategyHistoryOperation operation,
        String title,
        String description,
        String category,
        String campaign,
        LocalDate startsOn,
        LocalDate endsOn,
        boolean archived,
        String changedByUserId,
        Instant changedAt) {

    static StrategyHistoryResponse from(StrategyRevisionSnapshot snapshot) {
        return new StrategyHistoryResponse(
                snapshot.strategyId(), snapshot.revision(), snapshot.operation(), snapshot.title(),
                snapshot.description(), snapshot.category(), snapshot.campaign(), snapshot.startsOn(),
                snapshot.endsOn(), snapshot.archived(), snapshot.changedByUserId(), snapshot.changedAt());
    }
}
