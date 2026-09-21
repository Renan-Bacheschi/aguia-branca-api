package io.github.renanbacheschi.aguiabranca.idea;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ideas")
@CompoundIndex(name = "idea_author_status_idx", def = "{'authorUserId': 1, 'status': 1, 'updatedAt': -1}")
@CompoundIndex(name = "idea_review_idx", def = "{'status': 1, 'priority': 1, 'strategyId': 1}")
public class IdeaDocument {

    @Id
    private String id;
    private String title;
    private String problem;
    private String proposedSolution;
    private String expectedBenefits;
    private String strategyId;
    private String authorUserId;
    private IdeaStatus status;
    private IdeaPriority priority;
    private String reviewJustification;
    private String reviewedByUserId;
    private Instant submittedAt;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long internalVersion;

    protected IdeaDocument() {
    }

    public static IdeaDocument create(
            String title,
            String problem,
            String proposedSolution,
            String expectedBenefits,
            String strategyId,
            String authorUserId,
            Instant now) {
        IdeaDocument idea = new IdeaDocument();
        idea.title = title;
        idea.problem = problem;
        idea.proposedSolution = proposedSolution;
        idea.expectedBenefits = expectedBenefits;
        idea.strategyId = strategyId;
        idea.authorUserId = authorUserId;
        idea.status = IdeaStatus.DRAFT;
        idea.createdAt = now;
        idea.updatedAt = now;
        return idea;
    }

    public void update(
            String title,
            String problem,
            String proposedSolution,
            String expectedBenefits,
            String strategyId,
            Instant now) {
        this.title = title;
        this.problem = problem;
        this.proposedSolution = proposedSolution;
        this.expectedBenefits = expectedBenefits;
        this.strategyId = strategyId;
        this.updatedAt = now;
    }

    public void submit(Instant now) {
        this.status = IdeaStatus.SUBMITTED;
        this.submittedAt = now;
        this.updatedAt = now;
    }

    public void review(
            IdeaStatus decision,
            IdeaPriority priority,
            String justification,
            String reviewerUserId,
            Instant now) {
        this.status = decision;
        this.priority = priority;
        this.reviewJustification = justification;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getProblem() { return problem; }
    public String getProposedSolution() { return proposedSolution; }
    public String getExpectedBenefits() { return expectedBenefits; }
    public String getStrategyId() { return strategyId; }
    public String getAuthorUserId() { return authorUserId; }
    public IdeaStatus getStatus() { return status; }
    public IdeaPriority getPriority() { return priority; }
    public String getReviewJustification() { return reviewJustification; }
    public String getReviewedByUserId() { return reviewedByUserId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
