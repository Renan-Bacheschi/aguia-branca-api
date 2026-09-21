package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectUpdateRequest(
        @NotBlank(message = "O nome é obrigatório.") String name,
        @NotBlank(message = "A descrição é obrigatória.") String description,
        @NotBlank(message = "O responsável é obrigatório.") String responsibleName,
        @NotNull(message = "A data inicial planejada é obrigatória.") LocalDate plannedStartDate,
        @NotNull(message = "A data final planejada é obrigatória.") LocalDate plannedEndDate,
        @NotNull(message = "O investimento planejado é obrigatório.")
        @DecimalMin(value = "0.00", message = "O investimento planejado não pode ser negativo.")
        BigDecimal plannedInvestment) {
}
