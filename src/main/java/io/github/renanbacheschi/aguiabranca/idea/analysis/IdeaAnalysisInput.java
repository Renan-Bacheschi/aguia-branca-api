package io.github.renanbacheschi.aguiabranca.idea.analysis;

public record IdeaAnalysisInput(
        String title,
        String problem,
        String proposedSolution,
        String expectedBenefits,
        String strategyTitle,
        String strategyDescription,
        String strategyCategory,
        String strategyCampaign) {
}
