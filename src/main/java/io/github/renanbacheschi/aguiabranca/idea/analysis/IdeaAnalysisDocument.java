package io.github.renanbacheschi.aguiabranca.idea.analysis;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "idea_analyses")
@CompoundIndex(name = "idea_analysis_latest_idx", def = "{'ideaId': 1, 'generatedAt': -1}")
public class IdeaAnalysisDocument {

    @Id
    private String id;
    private String ideaId;
    private String strategyId;
    private String summary;
    private String strategicAlignment;
    private String potentialBenefits;
    private String risks;
    private String missingInformation;
    private String recommendation;
    private String provider;
    private String model;
    private String generatedByUserId;
    private Instant generatedAt;

    protected IdeaAnalysisDocument() {
    }

    public static IdeaAnalysisDocument create(
            String ideaId,
            String strategyId,
            ProviderIdeaAnalysis analysis,
            String generatedByUserId,
            Instant generatedAt) {
        IdeaAnalysisDocument document = new IdeaAnalysisDocument();
        document.ideaId = ideaId;
        document.strategyId = strategyId;
        document.summary = analysis.summary();
        document.strategicAlignment = analysis.strategicAlignment();
        document.potentialBenefits = analysis.potentialBenefits();
        document.risks = analysis.risks();
        document.missingInformation = analysis.missingInformation();
        document.recommendation = analysis.recommendation();
        document.provider = analysis.provider();
        document.model = analysis.model();
        document.generatedByUserId = generatedByUserId;
        document.generatedAt = generatedAt;
        return document;
    }

    public String getId() {
        return id;
    }

    public String getIdeaId() {
        return ideaId;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public String getSummary() {
        return summary;
    }

    public String getStrategicAlignment() {
        return strategicAlignment;
    }

    public String getPotentialBenefits() {
        return potentialBenefits;
    }

    public String getRisks() {
        return risks;
    }

    public String getMissingInformation() {
        return missingInformation;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getGeneratedByUserId() {
        return generatedByUserId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
