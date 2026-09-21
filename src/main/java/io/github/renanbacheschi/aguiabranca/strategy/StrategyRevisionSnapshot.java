package io.github.renanbacheschi.aguiabranca.strategy;

import java.time.Instant;
import java.time.LocalDate;

public record StrategyRevisionSnapshot(
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
}
