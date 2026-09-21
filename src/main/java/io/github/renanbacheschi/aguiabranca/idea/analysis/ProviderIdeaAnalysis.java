package io.github.renanbacheschi.aguiabranca.idea.analysis;

public record ProviderIdeaAnalysis(
        String summary,
        String strategicAlignment,
        String potentialBenefits,
        String risks,
        String missingInformation,
        String recommendation,
        String provider,
        String model) {
}
