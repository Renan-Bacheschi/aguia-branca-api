package io.github.renanbacheschi.aguiabranca.idea;

import jakarta.validation.constraints.NotBlank;

public record IdeaUpdateRequest(
        @NotBlank(message = "O título é obrigatório.") String title,
        @NotBlank(message = "O problema é obrigatório.") String problem,
        @NotBlank(message = "A solução proposta é obrigatória.") String proposedSolution,
        @NotBlank(message = "Os benefícios esperados são obrigatórios.") String expectedBenefits,
        @NotBlank(message = "A estratégia é obrigatória.") String strategyId) {
}
