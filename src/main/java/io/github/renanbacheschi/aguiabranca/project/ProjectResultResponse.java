package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProjectResultResponse(
        String id,
        String projectId,
        ResultType type,
        ResultNature nature,
        String description,
        LocalDate periodStart,
        LocalDate periodEnd,
        String unit,
        BigDecimal baselineValue,
        BigDecimal achievedValue,
        BigDecimal financialAmount,
        String recordedByUserId,
        Instant createdAt,
        Instant updatedAt) {

    static ProjectResultResponse from(ProjectResultDocument result) {
        return new ProjectResultResponse(
                result.getId(), result.getProjectId(), result.getType(), result.getNature(), result.getDescription(),
                result.getPeriodStart(), result.getPeriodEnd(), result.getUnit(), result.getBaselineValue(),
                result.getAchievedValue(), result.getFinancialAmount(), result.getRecordedByUserId(),
                result.getCreatedAt(), result.getUpdatedAt());
    }
}
