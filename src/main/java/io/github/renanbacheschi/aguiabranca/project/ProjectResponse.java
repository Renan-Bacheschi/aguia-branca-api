package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProjectResponse(
        String id,
        String ideaId,
        String strategyId,
        String name,
        String description,
        String responsibleName,
        ProjectStage stage,
        ProjectStatus status,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate actualEndDate,
        BigDecimal plannedInvestment,
        BigDecimal actualInvestment,
        int progressPercentage,
        boolean archived,
        String createdByUserId,
        String progressUpdatedByUserId,
        Instant createdAt,
        Instant updatedAt) {

    static ProjectResponse from(ProjectDocument project) {
        return new ProjectResponse(
                project.getId(), project.getIdeaId(), project.getStrategyId(), project.getName(),
                project.getDescription(), project.getResponsibleName(), project.getStage(), project.getStatus(),
                project.getPlannedStartDate(), project.getPlannedEndDate(), project.getActualEndDate(),
                project.getPlannedInvestment(), project.getActualInvestment(), project.getProgressPercentage(),
                project.isArchived(), project.getCreatedByUserId(), project.getProgressUpdatedByUserId(),
                project.getCreatedAt(), project.getUpdatedAt());
    }
}
