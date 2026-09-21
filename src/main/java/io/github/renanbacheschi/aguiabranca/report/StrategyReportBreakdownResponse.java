package io.github.renanbacheschi.aguiabranca.report;

import java.math.BigDecimal;

public record StrategyReportBreakdownResponse(
        String strategyId,
        String strategyTitle,
        String category,
        String campaign,
        boolean active,
        boolean archived,
        long ideaCount,
        long projectCount,
        long activeProjectCount,
        long archivedProjectCount,
        BigDecimal actualInvestment,
        BigDecimal actualFinancialBenefit,
        BigDecimal actualNetBenefit,
        BigDecimal actualRoi,
        boolean actualRoiAvailable) {
}
