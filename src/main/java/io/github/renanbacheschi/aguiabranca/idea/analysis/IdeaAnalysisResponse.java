package io.github.renanbacheschi.aguiabranca.idea.analysis;

import java.time.Instant;

public record IdeaAnalysisResponse(
        String ideaId,
        String strategyId,
        String summary,
        String strategicAlignment,
        String potentialBenefits,
        String risks,
        String missingInformation,
        String recommendation,
        Instant generatedAt,
        String generatedByUserId,
        String provider,
        String model) {

    static IdeaAnalysisResponse from(IdeaAnalysisDocument analysis) {
        return new IdeaAnalysisResponse(
                analysis.getIdeaId(), analysis.getStrategyId(), analysis.getSummary(),
                analysis.getStrategicAlignment(), analysis.getPotentialBenefits(), analysis.getRisks(),
                analysis.getMissingInformation(), analysis.getRecommendation(), analysis.getGeneratedAt(),
                analysis.getGeneratedByUserId(), analysis.getProvider(), analysis.getModel());
    }
}
