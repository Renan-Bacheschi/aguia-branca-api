package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "project_results")
@CompoundIndex(name = "project_result_period_idx", def = "{'projectId': 1, 'periodStart': -1, 'nature': 1}")
public class ProjectResultDocument {

    @Id
    private String id;
    private String projectId;
    private ResultType type;
    private ResultNature nature;
    private String description;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String unit;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal baselineValue;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal achievedValue;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal financialAmount;

    private String recordedByUserId;
    private Instant createdAt;
    private Instant updatedAt;

    protected ProjectResultDocument() {
    }

    public static ProjectResultDocument create(
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
            String userId,
            Instant now) {
        ProjectResultDocument result = new ProjectResultDocument();
        result.projectId = projectId;
        result.recordedByUserId = userId;
        result.createdAt = now;
        result.apply(type, nature, description, periodStart, periodEnd, unit,
                baselineValue, achievedValue, financialAmount, now);
        return result;
    }

    public void update(
            ResultType type,
            ResultNature nature,
            String description,
            LocalDate periodStart,
            LocalDate periodEnd,
            String unit,
            BigDecimal baselineValue,
            BigDecimal achievedValue,
            BigDecimal financialAmount,
            Instant now) {
        apply(type, nature, description, periodStart, periodEnd, unit,
                baselineValue, achievedValue, financialAmount, now);
    }

    private void apply(
            ResultType type,
            ResultNature nature,
            String description,
            LocalDate periodStart,
            LocalDate periodEnd,
            String unit,
            BigDecimal baselineValue,
            BigDecimal achievedValue,
            BigDecimal financialAmount,
            Instant now) {
        this.type = type;
        this.nature = nature;
        this.description = description;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.unit = unit;
        this.baselineValue = baselineValue;
        this.achievedValue = achievedValue;
        this.financialAmount = financialAmount;
        this.updatedAt = now;
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public ResultType getType() { return type; }
    public ResultNature getNature() { return nature; }
    public String getDescription() { return description; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public String getUnit() { return unit; }
    public BigDecimal getBaselineValue() { return baselineValue; }
    public BigDecimal getAchievedValue() { return achievedValue; }
    public BigDecimal getFinancialAmount() { return financialAmount; }
    public String getRecordedByUserId() { return recordedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
