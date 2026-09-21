package io.github.renanbacheschi.aguiabranca.strategy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "strategies")
@CompoundIndex(name = "strategy_period_idx", def = "{'archived': 1, 'startsOn': 1, 'endsOn': 1}")
public class StrategyDocument {

    @Id
    private String id;
    private String title;
    private String description;
    private String category;
    private String campaign;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private boolean archived;
    private long revision;
    private String createdByUserId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<StrategyRevisionSnapshot> history;

    @Version
    private Long internalVersion;

    protected StrategyDocument() {
    }

    public static StrategyDocument create(
            String title,
            String description,
            String category,
            String campaign,
            LocalDate startsOn,
            LocalDate endsOn,
            String userId,
            Instant now) {
        StrategyDocument strategy = new StrategyDocument();
        strategy.id = new ObjectId().toHexString();
        strategy.title = title;
        strategy.description = description;
        strategy.category = category;
        strategy.campaign = campaign;
        strategy.startsOn = startsOn;
        strategy.endsOn = endsOn;
        strategy.archived = false;
        strategy.revision = 1;
        strategy.createdByUserId = userId;
        strategy.createdAt = now;
        strategy.updatedAt = now;
        strategy.history = new ArrayList<>();
        strategy.appendSnapshot(StrategyHistoryOperation.CREATED, userId, now);
        return strategy;
    }

    public void update(
            String title,
            String description,
            String category,
            String campaign,
            LocalDate startsOn,
            LocalDate endsOn,
            String userId,
            Instant now) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.campaign = campaign;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.revision++;
        this.updatedAt = now;
        appendSnapshot(StrategyHistoryOperation.UPDATED, userId, now);
    }

    public void archive(String userId, Instant now) {
        this.archived = true;
        this.revision++;
        this.updatedAt = now;
        appendSnapshot(StrategyHistoryOperation.ARCHIVED, userId, now);
    }

    public boolean isActiveOn(LocalDate date) {
        return !archived && !date.isBefore(startsOn) && !date.isAfter(endsOn);
    }

    private void appendSnapshot(StrategyHistoryOperation operation, String userId, Instant now) {
        history.add(new StrategyRevisionSnapshot(
                id, revision, operation, title, description, category, campaign,
                startsOn, endsOn, archived, userId, now));
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getCampaign() {
        return campaign;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public boolean isArchived() {
        return archived;
    }

    public long getRevision() {
        return revision;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<StrategyRevisionSnapshot> getHistory() {
        return List.copyOf(history);
    }
}
