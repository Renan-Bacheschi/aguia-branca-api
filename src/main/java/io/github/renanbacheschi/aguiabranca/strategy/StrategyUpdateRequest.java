package io.github.renanbacheschi.aguiabranca.strategy;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StrategyUpdateRequest(
        @NotBlank(message = "O título é obrigatório.") String title,
        @NotBlank(message = "A descrição é obrigatória.") String description,
        @NotBlank(message = "A categoria é obrigatória.") String category,
        @NotBlank(message = "A campanha é obrigatória.") String campaign,
        @NotNull(message = "A data inicial é obrigatória.") LocalDate startsOn,
        @NotNull(message = "A data final é obrigatória.") LocalDate endsOn) {
}
