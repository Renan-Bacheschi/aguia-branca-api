package io.github.renanbacheschi.aguiabranca.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import io.github.renanbacheschi.aguiabranca.project.ProjectDocument;
import io.github.renanbacheschi.aguiabranca.project.ProjectResultDocument;
import io.github.renanbacheschi.aguiabranca.project.ResultNature;
import io.github.renanbacheschi.aguiabranca.project.ResultType;

@Component
public class ReportCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    FinancialCalculation calculateFinancials(
            List<ProjectDocument> projects,
            List<ProjectResultDocument> results) {
        BigDecimal plannedInvestment = projects.stream()
                .map(ProjectDocument::getPlannedInvestment)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualInvestment = projects.stream()
                .map(ProjectDocument::getActualInvestment)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal forecastFinancialBenefit = financialBenefit(results, ResultNature.FORECAST);
        BigDecimal actualFinancialBenefit = financialBenefit(results, ResultNature.ACTUAL);
        BigDecimal forecastNetBenefit = forecastFinancialBenefit.subtract(plannedInvestment);
        BigDecimal actualNetBenefit = actualFinancialBenefit.subtract(actualInvestment);

        return new FinancialCalculation(
                plannedInvestment,
                actualInvestment,
                forecastFinancialBenefit,
                actualFinancialBenefit,
                forecastNetBenefit,
                actualNetBenefit,
                calculatePercentage(forecastNetBenefit, plannedInvestment),
                calculatePercentage(actualNetBenefit, actualInvestment));
    }

    OperationalMetricResponse toOperationalMetric(ProjectResultDocument result) {
        RoiCalculation percentage = switch (result.getType()) {
            case PRODUCTIVITY_GAIN -> calculateOperationalPercentage(
                    result.getAchievedValue(), result.getBaselineValue(), result.getBaselineValue());
            case TIME_REDUCTION -> calculateOperationalPercentage(
                    result.getBaselineValue(), result.getAchievedValue(), result.getBaselineValue());
            case OTHER -> unavailablePercentage();
            case COST_SAVING, ADDITIONAL_REVENUE -> unavailablePercentage();
        };

        return new OperationalMetricResponse(
                result.getId(), result.getType(), result.getNature(), result.getDescription(),
                result.getPeriodStart(), result.getPeriodEnd(), result.getUnit(), result.getBaselineValue(),
                result.getAchievedValue(), percentage.value(), percentage.available());
    }

    boolean isOperational(ResultType type) {
        return type == ResultType.PRODUCTIVITY_GAIN
                || type == ResultType.TIME_REDUCTION
                || type == ResultType.OTHER;
    }

    private BigDecimal financialBenefit(List<ProjectResultDocument> results, ResultNature nature) {
        return results.stream()
                .filter(result -> result.getNature() == nature)
                .filter(result -> result.getType() == ResultType.COST_SAVING
                        || result.getType() == ResultType.ADDITIONAL_REVENUE)
                .map(ProjectResultDocument::getFinancialAmount)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private RoiCalculation calculateOperationalPercentage(
            BigDecimal minuend,
            BigDecimal subtrahend,
            BigDecimal baseline) {
        if (minuend == null || subtrahend == null || baseline == null) {
            return unavailablePercentage();
        }
        return calculatePercentage(minuend.subtract(subtrahend), baseline);
    }

    private RoiCalculation calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return unavailablePercentage();
        }
        BigDecimal percentage = numerator.multiply(ONE_HUNDRED)
                .divide(denominator, 2, RoundingMode.HALF_UP);
        return new RoiCalculation(percentage, true);
    }

    private RoiCalculation unavailablePercentage() {
        return new RoiCalculation(null, false);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
