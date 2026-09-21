package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectResultUpdateRequest(
        @NotNull(message = "O tipo é obrigatório.") ResultType type,
        @NotNull(message = "A natureza é obrigatória.") ResultNature nature,
        @NotBlank(message = "A descrição é obrigatória.") String description,
        @NotNull(message = "O início do período é obrigatório.") LocalDate periodStart,
        @NotNull(message = "O fim do período é obrigatório.") LocalDate periodEnd,
        String unit,
        BigDecimal baselineValue,
        BigDecimal achievedValue,
        @DecimalMin(value = "0.00", message = "O valor financeiro não pode ser negativo.")
        BigDecimal financialAmount) {
}
