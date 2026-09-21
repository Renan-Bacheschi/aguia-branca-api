package io.github.renanbacheschi.aguiabranca.idea;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @NotNull(message = "A decisão é obrigatória.") IdeaStatus decision,
        @NotNull(message = "A prioridade é obrigatória.") IdeaPriority priority,
        @NotBlank(message = "A justificativa é obrigatória.") String justification) {
}
