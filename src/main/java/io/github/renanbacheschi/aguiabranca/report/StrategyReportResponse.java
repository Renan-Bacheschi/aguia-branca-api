package io.github.renanbacheschi.aguiabranca.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.github.renanbacheschi.aguiabranca.idea.IdeaStatus;
import io.github.renanbacheschi.aguiabranca.project.ProjectStage;
import io.github.renanbacheschi.aguiabranca.project.ProjectStatus;

public record StrategyReportResponse(
        String strategyId,
        String strategyTitle,
        String strategyCategory,
        String strategyCampaign,
        boolean archived,
        boolean active,
        long ideaCount,
        Map<IdeaStatus, Long> ideasByStatus,
        long projectCount,
        long activeProjectCount,
        long archivedProjectCount,
        Map<ProjectStatus, Long> projectsByStatus,
        Map<ProjectStage, Long> projectsByStage,
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
        long resultCount,
        List<ProjectReportBreakdownResponse> projects,
        Instant generatedAt) {
}
