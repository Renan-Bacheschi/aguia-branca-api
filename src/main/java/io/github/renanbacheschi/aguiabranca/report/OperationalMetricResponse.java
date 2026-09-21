package io.github.renanbacheschi.aguiabranca.report;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.renanbacheschi.aguiabranca.project.ResultNature;
import io.github.renanbacheschi.aguiabranca.project.ResultType;

public record OperationalMetricResponse(
        String resultId,
        ResultType type,
        ResultNature nature,
        String description,
        LocalDate periodStart,
        LocalDate periodEnd,
        String unit,
        BigDecimal baselineValue,
        BigDecimal achievedValue,
        BigDecimal percentage,
        boolean percentageAvailable) {
}
