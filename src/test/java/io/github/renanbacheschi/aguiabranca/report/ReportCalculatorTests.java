package io.github.renanbacheschi.aguiabranca.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.renanbacheschi.aguiabranca.project.ProjectDocument;
import io.github.renanbacheschi.aguiabranca.project.ProjectResultDocument;
import io.github.renanbacheschi.aguiabranca.project.ProjectStage;
import io.github.renanbacheschi.aguiabranca.project.ProjectStatus;
import io.github.renanbacheschi.aguiabranca.project.ResultNature;
import io.github.renanbacheschi.aguiabranca.project.ResultType;

import static org.assertj.core.api.Assertions.assertThat;

class ReportCalculatorTests {

    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");
    private final ReportCalculator calculator = new ReportCalculator();

    @Test
    void actualReturnUsesActualInvestmentAndBenefit() {
        ProjectDocument project = project("10000.00", "10000.00");
        ProjectResultDocument result = financialResult(
                ResultType.COST_SAVING, ResultNature.ACTUAL, "15000.00");

        FinancialCalculation calculation = calculator.calculateFinancials(List.of(project), List.of(result));

        assertThat(calculation.actualFinancialBenefit()).isEqualByComparingTo("15000.00");
        assertThat(calculation.actualNetBenefit()).isEqualByComparingTo("5000.00");
        assertThat(calculation.actualRoi().available()).isTrue();
        assertThat(calculation.actualRoi().value()).isEqualByComparingTo("50.00");
    }

    @Test
    void zeroInvestmentMakesReturnUnavailable() {
        FinancialCalculation calculation = calculator.calculateFinancials(
                List.of(project("0", "0")),
                List.of(financialResult(ResultType.ADDITIONAL_REVENUE, ResultNature.ACTUAL, "100")));

        assertThat(calculation.actualRoi().available()).isFalse();
        assertThat(calculation.actualRoi().value()).isNull();
    }

    @Test
    void benefitBelowInvestmentProducesNegativeNetBenefitAndReturn() {
        FinancialCalculation calculation = calculator.calculateFinancials(
                List.of(project("1000", "1000")),
                List.of(financialResult(ResultType.COST_SAVING, ResultNature.ACTUAL, "600")));

        assertThat(calculation.actualNetBenefit()).isEqualByComparingTo("-400");
        assertThat(calculation.actualRoi().value()).isEqualByComparingTo("-40.00");
    }

    @Test
    void aggregateReturnUsesTotalsInsteadOfAverageProjectReturns() {
        ProjectDocument first = project("100", "100");
        ProjectDocument second = project("900", "900");
        ProjectResultDocument firstResult = financialResult(
                ResultType.COST_SAVING, ResultNature.ACTUAL, "150");
        ProjectResultDocument secondResult = financialResult(
                ResultType.ADDITIONAL_REVENUE, ResultNature.ACTUAL, "900");

        FinancialCalculation calculation = calculator.calculateFinancials(
                List.of(first, second), List.of(firstResult, secondResult));

        assertThat(calculation.actualInvestment()).isEqualByComparingTo("1000");
        assertThat(calculation.actualFinancialBenefit()).isEqualByComparingTo("1050");
        assertThat(calculation.actualRoi().value()).isEqualByComparingTo("5.00");
        assertThat(calculation.actualRoi().value()).isNotEqualByComparingTo("25.00");
    }

    @Test
    void forecastAndActualFinancialTypesAreSeparatedAndOperationalGainIsExcluded() {
        List<ProjectResultDocument> results = List.of(
                financialResult(ResultType.COST_SAVING, ResultNature.FORECAST, "200"),
                financialResult(ResultType.ADDITIONAL_REVENUE, ResultNature.ACTUAL, "300"),
                operationalResult(ResultType.PRODUCTIVITY_GAIN, ResultNature.ACTUAL, "items/hour", "20", "25"));

        FinancialCalculation calculation = calculator.calculateFinancials(
                List.of(project("100", "100")), results);

        assertThat(calculation.forecastFinancialBenefit()).isEqualByComparingTo("200");
        assertThat(calculation.actualFinancialBenefit()).isEqualByComparingTo("300");
    }

    @Test
    void operationalPercentagesFollowTypeFormulaAndHandleZeroBaseline() {
        OperationalMetricResponse productivity = calculator.toOperationalMetric(
                operationalResult(ResultType.PRODUCTIVITY_GAIN, ResultNature.ACTUAL, "items/hour", "20", "25"));
        OperationalMetricResponse timeReduction = calculator.toOperationalMetric(
                operationalResult(ResultType.TIME_REDUCTION, ResultNature.ACTUAL, "minutes", "100", "80"));
        OperationalMetricResponse unavailable = calculator.toOperationalMetric(
                operationalResult(ResultType.PRODUCTIVITY_GAIN, ResultNature.ACTUAL, "items/hour", "0", "25"));

        assertThat(productivity.percentage()).isEqualByComparingTo("25.00");
        assertThat(productivity.percentageAvailable()).isTrue();
        assertThat(timeReduction.percentage()).isEqualByComparingTo("20.00");
        assertThat(timeReduction.percentageAvailable()).isTrue();
        assertThat(unavailable.percentage()).isNull();
        assertThat(unavailable.percentageAvailable()).isFalse();
    }

    private ProjectDocument project(String plannedInvestment, String actualInvestment) {
        ProjectDocument project = ProjectDocument.create(
                "idea-1", "strategy-1", "Project", "Description", "Responsible",
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-12-31"),
                new BigDecimal(plannedInvestment), "manager-1", NOW);
        project.updateProgress(
                ProjectStage.EXECUTION, ProjectStatus.IN_PROGRESS, 50,
                new BigDecimal(actualInvestment), null, "manager-1", NOW);
        return project;
    }

    private ProjectResultDocument financialResult(ResultType type, ResultNature nature, String amount) {
        return ProjectResultDocument.create(
                "project-1", type, nature, "Financial result",
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"),
                null, null, null, new BigDecimal(amount), "manager-1", NOW);
    }

    private ProjectResultDocument operationalResult(
            ResultType type,
            ResultNature nature,
            String unit,
            String baseline,
            String achieved) {
        return ProjectResultDocument.create(
                "project-1", type, nature, "Operational result",
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"),
                unit, new BigDecimal(baseline), new BigDecimal(achieved), null, "manager-1", NOW);
    }
}
