package io.github.renanbacheschi.aguiabranca.report;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;
import io.github.renanbacheschi.aguiabranca.idea.IdeaDocument;
import io.github.renanbacheschi.aguiabranca.idea.IdeaPriority;
import io.github.renanbacheschi.aguiabranca.idea.IdeaRepository;
import io.github.renanbacheschi.aguiabranca.idea.IdeaStatus;
import io.github.renanbacheschi.aguiabranca.project.ProjectDocument;
import io.github.renanbacheschi.aguiabranca.project.ProjectRepository;
import io.github.renanbacheschi.aguiabranca.project.ProjectResultDocument;
import io.github.renanbacheschi.aguiabranca.project.ProjectResultRepository;
import io.github.renanbacheschi.aguiabranca.project.ProjectStage;
import io.github.renanbacheschi.aguiabranca.project.ProjectStatus;
import io.github.renanbacheschi.aguiabranca.project.ResultNature;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyDocument;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyRepository;

@Service
public class ReportService {

    private static final Comparator<ProjectDocument> PROJECT_ORDER = Comparator
            .comparing(ProjectDocument::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(ProjectDocument::getId, Comparator.nullsLast(String::compareTo));
    private static final Comparator<StrategyDocument> STRATEGY_ORDER = Comparator
            .comparing(StrategyDocument::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(StrategyDocument::getId, Comparator.nullsLast(String::compareTo));
    private static final Comparator<ProjectResultDocument> RESULT_ORDER = Comparator
            .comparing(ProjectResultDocument::getPeriodStart, Comparator.nullsLast(LocalDate::compareTo))
            .thenComparing(ProjectResultDocument::getId, Comparator.nullsLast(String::compareTo));

    private final StrategyRepository strategyRepository;
    private final IdeaRepository ideaRepository;
    private final ProjectRepository projectRepository;
    private final ProjectResultRepository resultRepository;
    private final ReportCalculator reportCalculator;
    private final Clock clock;

    public ReportService(
            StrategyRepository strategyRepository,
            IdeaRepository ideaRepository,
            ProjectRepository projectRepository,
            ProjectResultRepository resultRepository,
            ReportCalculator reportCalculator,
            Clock clock) {
        this.strategyRepository = strategyRepository;
        this.ideaRepository = ideaRepository;
        this.projectRepository = projectRepository;
        this.resultRepository = resultRepository;
        this.reportCalculator = reportCalculator;
        this.clock = clock;
    }

    public ProjectReportResponse getProjectReport(String projectId) {
        ProjectDocument project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado."));
        List<ProjectResultDocument> results = resultRepository.findAllByProjectId(projectId);
        return buildProjectReport(project, results, clock.instant());
    }

    public StrategyReportResponse getStrategyReport(String strategyId) {
        StrategyDocument strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new ResourceNotFoundException("Estratégia não encontrada."));
        List<IdeaDocument> ideas = ideaRepository.findAllByStrategyId(strategyId);
        List<ProjectDocument> projects = projectRepository.findAllByStrategyId(strategyId);
        List<ProjectResultDocument> results = findResults(projects);
        Instant generatedAt = clock.instant();
        return buildStrategyReport(
                strategy, ideas, projects, results, LocalDate.now(clock), generatedAt);
    }

    public SummaryReportResponse getSummaryReport() {
        List<StrategyDocument> strategies = strategyRepository.findAll();
        List<IdeaDocument> ideas = ideaRepository.findAll();
        List<ProjectDocument> projects = projectRepository.findAll();
        List<ProjectResultDocument> results = resultRepository.findAll();
        LocalDate currentDate = LocalDate.now(clock);
        Instant generatedAt = clock.instant();

        Map<String, List<IdeaDocument>> ideasByStrategy = ideas.stream()
                .collect(Collectors.groupingBy(IdeaDocument::getStrategyId));
        Map<String, List<ProjectDocument>> projectsByStrategy = projects.stream()
                .collect(Collectors.groupingBy(ProjectDocument::getStrategyId));
        Map<String, List<ProjectResultDocument>> resultsByProject = results.stream()
                .collect(Collectors.groupingBy(ProjectResultDocument::getProjectId));

        List<StrategyReportBreakdownResponse> strategyBreakdown = strategies.stream()
                .sorted(STRATEGY_ORDER)
                .map(strategy -> {
                    List<IdeaDocument> strategyIdeas = ideasByStrategy.getOrDefault(strategy.getId(), List.of());
                    List<ProjectDocument> strategyProjects = projectsByStrategy.getOrDefault(
                            strategy.getId(), List.of());
                    List<ProjectResultDocument> strategyResults = strategyProjects.stream()
                            .flatMap(project -> resultsByProject.getOrDefault(project.getId(), List.of()).stream())
                            .toList();
                    return buildStrategyBreakdown(
                            strategy, strategyIdeas, strategyProjects, strategyResults, currentDate);
                })
                .toList();

        FinancialCalculation financials = reportCalculator.calculateFinancials(projects, results);
        return new SummaryReportResponse(
                strategies.size(),
                strategies.stream().filter(strategy -> strategy.isActiveOn(currentDate)).count(),
                strategies.stream().filter(StrategyDocument::isArchived).count(),
                ideas.size(),
                countIdeasByStatus(ideas),
                countIdeasByPriority(ideas),
                projects.size(),
                projects.stream().filter(this::isActiveProject).count(),
                projects.stream().filter(ProjectDocument::isArchived).count(),
                countProjectsByStatus(projects),
                countProjectsByStage(projects),
                results.size(),
                countResults(results, ResultNature.FORECAST),
                countResults(results, ResultNature.ACTUAL),
                financials.plannedInvestment(),
                financials.actualInvestment(),
                financials.forecastFinancialBenefit(),
                financials.actualFinancialBenefit(),
                financials.forecastNetBenefit(),
                financials.actualNetBenefit(),
                financials.forecastRoi().value(),
                financials.forecastRoi().available(),
                financials.actualRoi().value(),
                financials.actualRoi().available(),
                strategyBreakdown,
                generatedAt);
    }

    private ProjectReportResponse buildProjectReport(
            ProjectDocument project,
            List<ProjectResultDocument> results,
            Instant generatedAt) {
        FinancialCalculation financials = reportCalculator.calculateFinancials(List.of(project), results);
        List<OperationalMetricResponse> operationalMetrics = results.stream()
                .filter(result -> reportCalculator.isOperational(result.getType()))
                .sorted(RESULT_ORDER)
                .map(reportCalculator::toOperationalMetric)
                .toList();
        return new ProjectReportResponse(
                project.getId(), project.getName(), project.getStrategyId(), project.getIdeaId(),
                project.getStage(), project.getStatus(), project.getProgressPercentage(), project.isArchived(),
                financials.plannedInvestment(), financials.actualInvestment(),
                financials.forecastFinancialBenefit(), financials.actualFinancialBenefit(),
                financials.forecastNetBenefit(), financials.actualNetBenefit(),
                financials.forecastRoi().value(), financials.forecastRoi().available(),
                financials.actualRoi().value(), financials.actualRoi().available(),
                results.size(), countResults(results, ResultNature.FORECAST),
                countResults(results, ResultNature.ACTUAL), operationalMetrics, generatedAt);
    }

    private StrategyReportResponse buildStrategyReport(
            StrategyDocument strategy,
            List<IdeaDocument> ideas,
            List<ProjectDocument> projects,
            List<ProjectResultDocument> results,
            LocalDate currentDate,
            Instant generatedAt) {
        Map<String, List<ProjectResultDocument>> resultsByProject = results.stream()
                .collect(Collectors.groupingBy(ProjectResultDocument::getProjectId));
        List<ProjectReportBreakdownResponse> projectBreakdown = projects.stream()
                .sorted(PROJECT_ORDER)
                .map(project -> buildProjectBreakdown(
                        project, resultsByProject.getOrDefault(project.getId(), List.of())))
                .toList();
        FinancialCalculation financials = reportCalculator.calculateFinancials(projects, results);

        return new StrategyReportResponse(
                strategy.getId(), strategy.getTitle(), strategy.getCategory(), strategy.getCampaign(),
                strategy.isArchived(), strategy.isActiveOn(currentDate), ideas.size(), countIdeasByStatus(ideas),
                projects.size(), projects.stream().filter(this::isActiveProject).count(),
                projects.stream().filter(ProjectDocument::isArchived).count(),
                countProjectsByStatus(projects), countProjectsByStage(projects),
                financials.plannedInvestment(), financials.actualInvestment(),
                financials.forecastFinancialBenefit(), financials.actualFinancialBenefit(),
                financials.forecastNetBenefit(), financials.actualNetBenefit(),
                financials.forecastRoi().value(), financials.forecastRoi().available(),
                financials.actualRoi().value(), financials.actualRoi().available(),
                results.size(), projectBreakdown, generatedAt);
    }

    private ProjectReportBreakdownResponse buildProjectBreakdown(
            ProjectDocument project,
            List<ProjectResultDocument> results) {
        FinancialCalculation financials = reportCalculator.calculateFinancials(List.of(project), results);
        return new ProjectReportBreakdownResponse(
                project.getId(), project.getName(), project.getStatus(), project.getStage(),
                project.getProgressPercentage(), project.isArchived(), financials.actualInvestment(),
                financials.actualFinancialBenefit(), financials.actualNetBenefit(),
                financials.actualRoi().value(), financials.actualRoi().available());
    }

    private StrategyReportBreakdownResponse buildStrategyBreakdown(
            StrategyDocument strategy,
            List<IdeaDocument> ideas,
            List<ProjectDocument> projects,
            List<ProjectResultDocument> results,
            LocalDate currentDate) {
        FinancialCalculation financials = reportCalculator.calculateFinancials(projects, results);
        return new StrategyReportBreakdownResponse(
                strategy.getId(), strategy.getTitle(), strategy.getCategory(), strategy.getCampaign(),
                strategy.isActiveOn(currentDate), strategy.isArchived(), ideas.size(), projects.size(),
                projects.stream().filter(this::isActiveProject).count(),
                projects.stream().filter(ProjectDocument::isArchived).count(),
                financials.actualInvestment(), financials.actualFinancialBenefit(),
                financials.actualNetBenefit(), financials.actualRoi().value(),
                financials.actualRoi().available());
    }

    private List<ProjectResultDocument> findResults(List<ProjectDocument> projects) {
        List<String> projectIds = projects.stream().map(ProjectDocument::getId).toList();
        return projectIds.isEmpty() ? List.of() : resultRepository.findAllByProjectIdIn(projectIds);
    }

    private Map<IdeaStatus, Long> countIdeasByStatus(List<IdeaDocument> ideas) {
        return countEnums(IdeaStatus.class, ideas, IdeaDocument::getStatus);
    }

    private Map<String, Long> countIdeasByPriority(List<IdeaDocument> ideas) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("UNASSIGNED", 0L);
        for (IdeaPriority priority : IdeaPriority.values()) counts.put(priority.name(), 0L);
        for (IdeaDocument idea : ideas) {
            String key = idea.getPriority() == null ? "UNASSIGNED" : idea.getPriority().name();
            counts.compute(key, (ignored, count) -> count + 1);
        }
        return counts;
    }

    private Map<ProjectStatus, Long> countProjectsByStatus(List<ProjectDocument> projects) {
        return countEnums(ProjectStatus.class, projects, ProjectDocument::getStatus);
    }

    private Map<ProjectStage, Long> countProjectsByStage(List<ProjectDocument> projects) {
        return countEnums(ProjectStage.class, projects, ProjectDocument::getStage);
    }

    private <E extends Enum<E>, T> Map<E, Long> countEnums(
            Class<E> enumType,
            List<T> items,
            Function<T, E> classifier) {
        Map<E, Long> counts = new EnumMap<>(enumType);
        for (E value : enumType.getEnumConstants()) counts.put(value, 0L);
        for (T item : items) counts.compute(classifier.apply(item), (ignored, count) -> count + 1);
        return counts;
    }

    private long countResults(List<ProjectResultDocument> results, ResultNature nature) {
        return results.stream().filter(result -> result.getNature() == nature).count();
    }

    private boolean isActiveProject(ProjectDocument project) {
        return !project.isArchived()
                && (project.getStatus() == ProjectStatus.PLANNED
                || project.getStatus() == ProjectStatus.IN_PROGRESS);
    }
}
