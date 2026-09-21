package io.github.renanbacheschi.aguiabranca.report;

import java.math.BigDecimal;

record FinancialCalculation(
        BigDecimal plannedInvestment,
        BigDecimal actualInvestment,
        BigDecimal forecastFinancialBenefit,
        BigDecimal actualFinancialBenefit,
        BigDecimal forecastNetBenefit,
        BigDecimal actualNetBenefit,
        RoiCalculation forecastRoi,
        RoiCalculation actualRoi) {
}
