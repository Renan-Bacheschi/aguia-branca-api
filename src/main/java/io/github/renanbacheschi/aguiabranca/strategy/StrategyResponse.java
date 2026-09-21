package io.github.renanbacheschi.aguiabranca.strategy;

import java.time.Instant;
import java.time.LocalDate;

public record StrategyResponse(
        String id,
        String title,
        String description,
        String category,
        String campaign,
        LocalDate startsOn,
        LocalDate endsOn,
        boolean active,
        boolean archived,
        long revision,
        String createdByUserId,
        Instant createdAt,
        Instant updatedAt) {

    static StrategyResponse from(StrategyDocument strategy, LocalDate currentDate) {
        return new StrategyResponse(
                strategy.getId(), strategy.getTitle(), strategy.getDescription(), strategy.getCategory(),
                strategy.getCampaign(), strategy.getStartsOn(), strategy.getEndsOn(),
                strategy.isActiveOn(currentDate), strategy.isArchived(), strategy.getRevision(),
                strategy.getCreatedByUserId(), strategy.getCreatedAt(), strategy.getUpdatedAt());
    }
}
