package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProgressUpdateRequest(
        @NotNull(message = "A etapa é obrigatória.") ProjectStage stage,
        @NotNull(message = "O status é obrigatório.") ProjectStatus status,
        @NotNull(message = "O percentual de progresso é obrigatório.")
        @Min(value = 0, message = "O progresso deve ser no mínimo 0.")
        @Max(value = 100, message = "O progresso deve ser no máximo 100.")
        Integer progressPercentage,
        @DecimalMin(value = "0.00", message = "O investimento realizado não pode ser negativo.")
        BigDecimal actualInvestment,
        LocalDate actualEndDate) {
}
