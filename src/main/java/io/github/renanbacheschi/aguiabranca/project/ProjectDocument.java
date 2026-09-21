package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "projects")
@CompoundIndex(name = "project_filters_idx", def = "{'archived': 1, 'strategyId': 1, 'status': 1, 'stage': 1}")
public class ProjectDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String ideaId;

    private String strategyId;
    private String name;
    private String description;
    private String responsibleName;
    private ProjectStage stage;
    private ProjectStatus status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualEndDate;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal plannedInvestment;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal actualInvestment;

    private int progressPercentage;
    private boolean archived;
    private String createdByUserId;
    private String progressUpdatedByUserId;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long internalVersion;

    protected ProjectDocument() {
    }

    public static ProjectDocument create(
            String ideaId,
            String strategyId,
            String name,
            String description,
            String responsibleName,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            BigDecimal plannedInvestment,
            String userId,
            Instant now) {
        ProjectDocument project = new ProjectDocument();
        project.ideaId = ideaId;
        project.strategyId = strategyId;
        project.name = name;
        project.description = description;
        project.responsibleName = responsibleName;
        project.stage = ProjectStage.PLANNING;
        project.status = ProjectStatus.PLANNED;
        project.plannedStartDate = plannedStartDate;
        project.plannedEndDate = plannedEndDate;
        project.plannedInvestment = plannedInvestment;
        project.actualInvestment = BigDecimal.ZERO;
        project.progressPercentage = 0;
        project.archived = false;
        project.createdByUserId = userId;
        project.createdAt = now;
        project.updatedAt = now;
        return project;
    }

    public void update(
            String name,
            String description,
            String responsibleName,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            BigDecimal plannedInvestment,
            Instant now) {
        this.name = name;
        this.description = description;
        this.responsibleName = responsibleName;
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
        this.plannedInvestment = plannedInvestment;
        this.updatedAt = now;
    }

    public void updateProgress(
            ProjectStage stage,
            ProjectStatus status,
            int progressPercentage,
            BigDecimal actualInvestment,
            LocalDate actualEndDate,
            String userId,
            Instant now) {
        this.stage = stage;
        this.status = status;
        this.progressPercentage = progressPercentage;
        this.actualInvestment = actualInvestment;
        this.actualEndDate = actualEndDate;
        this.progressUpdatedByUserId = userId;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        this.archived = true;
        this.updatedAt = now;
    }

    public String getId() { return id; }
    public String getIdeaId() { return ideaId; }
    public String getStrategyId() { return strategyId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getResponsibleName() { return responsibleName; }
    public ProjectStage getStage() { return stage; }
    public ProjectStatus getStatus() { return status; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public LocalDate getActualEndDate() { return actualEndDate; }
    public BigDecimal getPlannedInvestment() { return plannedInvestment; }
    public BigDecimal getActualInvestment() { return actualInvestment; }
    public int getProgressPercentage() { return progressPercentage; }
    public boolean isArchived() { return archived; }
    public String getCreatedByUserId() { return createdByUserId; }
    public String getProgressUpdatedByUserId() { return progressUpdatedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
