package io.github.renanbacheschi.aguiabranca.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import io.github.renanbacheschi.aguiabranca.project.ProjectStage;
import io.github.renanbacheschi.aguiabranca.project.ProjectStatus;

public record ProjectReportResponse(
        String projectId,
        String projectName,
        String strategyId,
        String ideaId,
        ProjectStage stage,
        ProjectStatus status,
        int progressPercentage,
        boolean archived,
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
        long forecastResultCount,
        long actualResultCount,
        List<OperationalMetricResponse> operationalMetrics,
        Instant generatedAt) {
}
