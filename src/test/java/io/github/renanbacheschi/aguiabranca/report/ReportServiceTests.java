package io.github.renanbacheschi.aguiabranca.report;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;
import io.github.renanbacheschi.aguiabranca.idea.IdeaDocument;
import io.github.renanbacheschi.aguiabranca.idea.IdeaRepository;
import io.github.renanbacheschi.aguiabranca.idea.IdeaStatus;
import io.github.renanbacheschi.aguiabranca.project.ProjectDocument;
import io.github.renanbacheschi.aguiabranca.project.ProjectRepository;
import io.github.renanbacheschi.aguiabranca.project.ProjectResultDocument;
import io.github.renanbacheschi.aguiabranca.project.ProjectResultRepository;
import io.github.renanbacheschi.aguiabranca.project.ProjectStage;
import io.github.renanbacheschi.aguiabranca.project.ProjectStatus;
import io.github.renanbacheschi.aguiabranca.project.ResultNature;
import io.github.renanbacheschi.aguiabranca.project.ResultType;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyDocument;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");

    @Mock
    private StrategyRepository strategyRepository;
    @Mock
    private IdeaRepository ideaRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectResultRepository resultRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                strategyRepository, ideaRepository, projectRepository, resultRepository,
                new ReportCalculator(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void projectWithoutResultsKeepsInvestmentsAndReturnsZeroBenefits() {
        ProjectDocument project = project("project-1", "strategy-1", "10000", "4000");
        when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));
        when(resultRepository.findAllByProjectId("project-1")).thenReturn(List.of());

        ProjectReportResponse response = reportService.getProjectReport("project-1");

        assertThat(response.plannedInvestment()).isEqualByComparingTo("10000");
        assertThat(response.actualInvestment()).isEqualByComparingTo("4000");
        assertThat(response.forecastFinancialBenefit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.actualFinancialBenefit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.resultCount()).isZero();
        assertThat(response.operationalMetrics()).isEmpty();
        assertThat(response.generatedAt()).isEqualTo(NOW);
    }

    @Test
    void strategyWithoutProjectsReturnsCompleteZeroGroupsAndEmptyBreakdown() {
        StrategyDocument strategy = strategy();
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));
        when(ideaRepository.findAllByStrategyId(strategy.getId())).thenReturn(List.of());
        when(projectRepository.findAllByStrategyId(strategy.getId())).thenReturn(List.of());

        StrategyReportResponse response = reportService.getStrategyReport(strategy.getId());

        assertThat(response.projectCount()).isZero();
        assertThat(response.plannedInvestment()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.actualFinancialBenefit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.projects()).isEmpty();
        assertThat(response.ideasByStatus()).containsOnlyKeys(IdeaStatus.values());
        assertThat(response.ideasByStatus().values()).containsOnly(0L);
        verify(resultRepository, never()).findAllByProjectIdIn(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archivedStrategyAndProjectRemainAvailableWithHistoricalValues() {
        StrategyDocument strategy = strategy();
        strategy.archive("leader-1", NOW);
        ProjectDocument project = project("project-1", strategy.getId(), "10000", "10000");
        project.archive(NOW);
        ProjectResultDocument result = financialResult("project-1", "15000");
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));
        when(ideaRepository.findAllByStrategyId(strategy.getId())).thenReturn(List.of());
        when(projectRepository.findAllByStrategyId(strategy.getId())).thenReturn(List.of(project));
        when(resultRepository.findAllByProjectIdIn(List.of("project-1"))).thenReturn(List.of(result));
        when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));
        when(resultRepository.findAllByProjectId("project-1")).thenReturn(List.of(result));

        StrategyReportResponse response = reportService.getStrategyReport(strategy.getId());
        ProjectReportResponse projectResponse = reportService.getProjectReport("project-1");

        assertThat(response.archived()).isTrue();
        assertThat(response.active()).isFalse();
        assertThat(response.archivedProjectCount()).isEqualTo(1);
        assertThat(response.activeProjectCount()).isZero();
        assertThat(response.actualNetBenefit()).isEqualByComparingTo("5000");
        assertThat(response.actualRoi()).isEqualByComparingTo("50.00");
        assertThat(response.projects()).singleElement().satisfies(item -> assertThat(item.archived()).isTrue());
        assertThat(projectResponse.archived()).isTrue();
        assertThat(projectResponse.actualNetBenefit()).isEqualByComparingTo("5000");
    }

    @Test
    void summaryIncludesEmptyStrategiesAndUnassignedIdeasWithDeterministicCounts() {
        StrategyDocument strategy = strategy();
        IdeaDocument idea = IdeaDocument.create(
                "Idea", "Problem", "Solution", "Benefits", strategy.getId(), "operator-1", NOW);
        when(strategyRepository.findAll()).thenReturn(List.of(strategy));
        when(ideaRepository.findAll()).thenReturn(List.of(idea));
        when(projectRepository.findAll()).thenReturn(List.of());
        when(resultRepository.findAll()).thenReturn(List.of());

        SummaryReportResponse response = reportService.getSummaryReport();

        assertThat(response.totalStrategies()).isEqualTo(1);
        assertThat(response.activeStrategies()).isEqualTo(1);
        assertThat(response.totalIdeas()).isEqualTo(1);
        assertThat(response.ideasByPriority().get("UNASSIGNED")).isEqualTo(1);
        assertThat(response.totalProjects()).isZero();
        assertThat(response.strategies()).singleElement().satisfies(item -> {
            assertThat(item.ideaCount()).isEqualTo(1);
            assertThat(item.projectCount()).isZero();
            assertThat(item.actualInvestment()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void missingStrategyAndProjectReturnNotFound() {
        when(strategyRepository.findById("missing-strategy")).thenReturn(Optional.empty());
        when(projectRepository.findById("missing-project")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getStrategyReport("missing-strategy"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> reportService.getProjectReport("missing-project"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private StrategyDocument strategy() {
        return StrategyDocument.create(
                "Strategy", "Description", "Category", "Campaign",
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"), "leader-1", NOW);
    }

    private ProjectDocument project(
            String id,
            String strategyId,
            String plannedInvestment,
            String actualInvestment) {
        ProjectDocument project = ProjectDocument.create(
                "idea-1", strategyId, "Project", "Description", "Responsible",
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-12-31"),
                new BigDecimal(plannedInvestment), "manager-1", NOW);
        ReflectionTestUtils.setField(project, "id", id);
        project.updateProgress(
                ProjectStage.EXECUTION, ProjectStatus.IN_PROGRESS, 50,
                new BigDecimal(actualInvestment), null, "manager-1", NOW);
        return project;
    }

    private ProjectResultDocument financialResult(String projectId, String amount) {
        return ProjectResultDocument.create(
                projectId, ResultType.COST_SAVING, ResultNature.ACTUAL, "Saving",
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"),
                null, null, null, new BigDecimal(amount), "manager-1", NOW);
    }
}
