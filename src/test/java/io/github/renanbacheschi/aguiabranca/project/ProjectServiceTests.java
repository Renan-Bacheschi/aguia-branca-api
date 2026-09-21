package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import io.github.renanbacheschi.aguiabranca.error.ConflictException;
import io.github.renanbacheschi.aguiabranca.error.InvalidRequestException;
import io.github.renanbacheschi.aguiabranca.idea.IdeaDocument;
import io.github.renanbacheschi.aguiabranca.idea.IdeaPriority;
import io.github.renanbacheschi.aguiabranca.idea.IdeaService;
import io.github.renanbacheschi.aguiabranca.idea.IdeaStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private IdeaService ideaService;
    @Mock
    private MongoTemplate mongoTemplate;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository, ideaService, mongoTemplate, Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creationRequiresApprovedIdeaAndInheritsStrategy() {
        when(ideaService.requireApproved("idea-1")).thenReturn(approvedIdea());

        ProjectResponse response = projectService.create(createRequest(), "manager-1");

        assertThat(response.ideaId()).isEqualTo("idea-1");
        assertThat(response.strategyId()).isEqualTo("strategy-1");
        assertThat(response.stage()).isEqualTo(ProjectStage.PLANNING);
        assertThat(response.status()).isEqualTo(ProjectStatus.PLANNED);
        assertThat(response.createdByUserId()).isEqualTo("manager-1");
        assertThat(response.actualInvestment()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void duplicateProjectForIdeaIsRejectedBeforeSave() {
        when(ideaService.requireApproved("idea-1")).thenReturn(approvedIdea());
        when(projectRepository.existsByIdeaId("idea-1")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(createRequest(), "manager-1"))
                .isInstanceOf(ConflictException.class);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void projectCannotBeCreatedFromIdeaThatIsNotApproved() {
        when(ideaService.requireApproved("idea-1"))
                .thenThrow(new ConflictException("O projeto deve ser criado a partir de uma ideia aprovada."));

        assertThatThrownBy(() -> projectService.create(createRequest(), "manager-1"))
                .isInstanceOf(ConflictException.class);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void invalidPlannedPeriodIsRejected() {
        ProjectCreateRequest invalid = new ProjectCreateRequest(
                "idea-1", "Name", "Description", "Responsible",
                LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-01"), BigDecimal.TEN);

        assertThatThrownBy(() -> projectService.create(invalid, "manager-1"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void progressMustStayBetweenCoherentStageAndStatusCombinations() {
        ProjectDocument project = project();
        when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.updateProgress("project-1", new ProgressUpdateRequest(
                ProjectStage.PLANNING, ProjectStatus.IN_PROGRESS, 50, BigDecimal.TEN, null), "manager-1"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void completedProjectRequiresFullProgressAndActualEndDate() {
        ProjectDocument project = project();
        when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.updateProgress("project-1", new ProgressUpdateRequest(
                ProjectStage.CLOSED, ProjectStatus.COMPLETED, 99, BigDecimal.TEN, null), "manager-1"))
                .isInstanceOf(InvalidRequestException.class);

        ProjectResponse completed = projectService.updateProgress("project-1", new ProgressUpdateRequest(
                ProjectStage.CLOSED, ProjectStatus.COMPLETED, 100, BigDecimal.TEN,
                LocalDate.parse("2026-10-10")), "manager-1");
        assertThat(completed.progressPercentage()).isEqualTo(100);
        assertThat(completed.actualEndDate()).isEqualTo(LocalDate.parse("2026-10-10"));
        assertThat(completed.progressUpdatedByUserId()).isEqualTo("manager-1");
    }

    @Test
    void archivedProjectRejectsFurtherChanges() {
        ProjectDocument project = project();
        when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));

        projectService.archive("project-1");

        assertThat(project.isArchived()).isTrue();
        assertThatThrownBy(() -> projectService.update("project-1", new ProjectUpdateRequest(
                "Name", "Description", "Responsible", LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-20"), BigDecimal.TEN)))
                .isInstanceOf(ConflictException.class);
    }

    private ProjectCreateRequest createRequest() {
        return new ProjectCreateRequest(
                "idea-1", "Name", "Description", "Responsible",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"), new BigDecimal("1000.00"));
    }

    private IdeaDocument approvedIdea() {
        IdeaDocument idea = IdeaDocument.create(
                "Title", "Problem", "Solution", "Benefits", "strategy-1", "operator-1", NOW);
        idea.submit(NOW);
        idea.review(IdeaStatus.APPROVED, IdeaPriority.HIGH, "Approved", "manager-1", NOW);
        return idea;
    }

    private ProjectDocument project() {
        return ProjectDocument.create(
                "idea-1", "strategy-1", "Name", "Description", "Responsible",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"),
                new BigDecimal("1000.00"), "manager-1", NOW);
    }
}
