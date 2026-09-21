package io.github.renanbacheschi.aguiabranca.idea;

import java.time.Instant;

public record IdeaResponse(
        String id,
        String title,
        String problem,
        String proposedSolution,
        String expectedBenefits,
        String strategyId,
        String authorUserId,
        IdeaStatus status,
        IdeaPriority priority,
        String reviewJustification,
        String reviewedByUserId,
        Instant submittedAt,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt) {

    static IdeaResponse from(IdeaDocument idea) {
        return new IdeaResponse(
                idea.getId(), idea.getTitle(), idea.getProblem(), idea.getProposedSolution(),
                idea.getExpectedBenefits(), idea.getStrategyId(), idea.getAuthorUserId(), idea.getStatus(),
                idea.getPriority(), idea.getReviewJustification(), idea.getReviewedByUserId(),
                idea.getSubmittedAt(), idea.getReviewedAt(), idea.getCreatedAt(), idea.getUpdatedAt());
    }
}
