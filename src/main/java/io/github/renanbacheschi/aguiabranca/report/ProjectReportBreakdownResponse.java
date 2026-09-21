package io.github.renanbacheschi.aguiabranca.report;

import java.math.BigDecimal;

import io.github.renanbacheschi.aguiabranca.project.ProjectStage;
import io.github.renanbacheschi.aguiabranca.project.ProjectStatus;

public record ProjectReportBreakdownResponse(
        String projectId,
        String projectName,
        ProjectStatus status,
        ProjectStage stage,
        int progressPercentage,
        boolean archived,
        BigDecimal actualInvestment,
        BigDecimal actualFinancialBenefit,
        BigDecimal actualNetBenefit,
        BigDecimal actualRoi,
        boolean actualRoiAvailable) {
}
