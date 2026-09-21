package io.github.renanbacheschi.aguiabranca.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.github.renanbacheschi.aguiabranca.idea.IdeaStatus;
import io.github.renanbacheschi.aguiabranca.project.ProjectStage;
import io.github.renanbacheschi.aguiabranca.project.ProjectStatus;

public record SummaryReportResponse(
        long totalStrategies,
        long activeStrategies,
        long archivedStrategies,
        long totalIdeas,
        Map<IdeaStatus, Long> ideasByStatus,
        Map<String, Long> ideasByPriority,
        long totalProjects,
        long activeProjectCount,
        long archivedProjectCount,
        Map<ProjectStatus, Long> projectsByStatus,
        Map<ProjectStage, Long> projectsByStage,
        long totalResults,
        long forecastResultCount,
        long actualResultCount,
        BigDecimal plannedInvestment,
        BigDecimal actualInvestment,
        BigDecimal forecastFinancialBenefit,
        BigDecimal actualFinancialBenefit,
        BigDecimal forecastNetBenefit,
        BigDecimal actualNetBenefit,
        BigDecimal forecastRoi,
        boolean forecastRoiAvailable,
        BigDecimal actualRoi,
        boolean actualRoiAvailable,
        List<StrategyReportBreakdownResponse> strategies,
        Instant generatedAt) {
}
