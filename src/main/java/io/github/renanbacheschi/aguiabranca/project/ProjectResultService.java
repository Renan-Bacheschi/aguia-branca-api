package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.Clock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import io.github.renanbacheschi.aguiabranca.common.PageResponse;
import io.github.renanbacheschi.aguiabranca.error.InvalidRequestException;
import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;

@Service
public class ProjectResultService {

    private final ProjectResultRepository resultRepository;
    private final ProjectService projectService;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public ProjectResultService(
            ProjectResultRepository resultRepository,
            ProjectService projectService,
            MongoTemplate mongoTemplate,
            Clock clock) {
        this.resultRepository = resultRepository;
        this.projectService = projectService;
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public ProjectResultResponse create(String projectId, ProjectResultCreateRequest request, String userId) {
        projectService.requireResultMutationAllowed(projectId);
        validate(request.type(), request.periodStart(), request.periodEnd(), request.unit(),
                request.baselineValue(), request.achievedValue(), request.financialAmount());
        ProjectResultDocument result = ProjectResultDocument.create(
                projectId, request.type(), request.nature(), clean(request.description()), request.periodStart(),
                request.periodEnd(), cleanNullable(request.unit()), request.baselineValue(), request.achievedValue(),
                request.financialAmount(), userId, clock.instant());
        return ProjectResultResponse.from(resultRepository.save(result));
    }

    public PageResponse<ProjectResultResponse> list(String projectId, int page, int size) {
        validatePage(page, size);
        projectService.requireProject(projectId);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "periodStart", "createdAt"));
        Query query = Query.query(Criteria.where("projectId").is(projectId)).with(pageable);
        long total = mongoTemplate.count(Query.query(Criteria.where("projectId").is(projectId)), ProjectResultDocument.class);
        Page<ProjectResultDocument> results = new org.springframework.data.domain.PageImpl<>(
                mongoTemplate.find(query, ProjectResultDocument.class), pageable, total);
        return PageResponse.from(results, ProjectResultResponse::from);
    }

    public ProjectResultResponse get(String projectId, String resultId) {
        projectService.requireProject(projectId);
        return ProjectResultResponse.from(requireNested(projectId, resultId));
    }

    public ProjectResultResponse update(String projectId, String resultId, ProjectResultUpdateRequest request) {
        projectService.requireResultMutationAllowed(projectId);
        validate(request.type(), request.periodStart(), request.periodEnd(), request.unit(),
                request.baselineValue(), request.achievedValue(), request.financialAmount());
        ProjectResultDocument result = requireNested(projectId, resultId);
        result.update(
                request.type(), request.nature(), clean(request.description()), request.periodStart(),
                request.periodEnd(), cleanNullable(request.unit()), request.baselineValue(), request.achievedValue(),
                request.financialAmount(), clock.instant());
        return ProjectResultResponse.from(resultRepository.save(result));
    }

    public void delete(String projectId, String resultId) {
        projectService.requireResultMutationAllowed(projectId);
        resultRepository.delete(requireNested(projectId, resultId));
    }

    private ProjectResultDocument requireNested(String projectId, String resultId) {
        ProjectResultDocument result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Resultado não encontrado."));
        if (!projectId.equals(result.getProjectId())) {
            throw new ResourceNotFoundException("Resultado não encontrado.");
        }
        return result;
    }

    private void validate(
            ResultType type,
            java.time.LocalDate periodStart,
            java.time.LocalDate periodEnd,
            String unit,
            BigDecimal baselineValue,
            BigDecimal achievedValue,
            BigDecimal financialAmount) {
        if (periodEnd.isBefore(periodStart)) {
            throw new InvalidRequestException("O fim do período não pode ser anterior ao início.");
        }
        boolean hasUnit = unit != null && !unit.isBlank();
        boolean hasBaseline = baselineValue != null;
        boolean hasAchieved = achievedValue != null;
        boolean completeOperationalMetric = hasUnit && hasBaseline && hasAchieved;
        boolean anyOperationalMetric = hasUnit || hasBaseline || hasAchieved;
        boolean financialType = type == ResultType.COST_SAVING || type == ResultType.ADDITIONAL_REVENUE;

        if (financialType && financialAmount == null) {
            throw new InvalidRequestException("Resultados financeiros exigem financialAmount.");
        }
        if (financialType && anyOperationalMetric && !completeOperationalMetric) {
            throw new InvalidRequestException("A métrica operacional deve informar unit, baselineValue e achievedValue.");
        }
        if (!financialType && !completeOperationalMetric) {
            throw new InvalidRequestException("Resultados operacionais exigem unit, baselineValue e achievedValue.");
        }
        if (!financialType && financialAmount != null) {
            throw new InvalidRequestException("financialAmount é permitido apenas para resultados financeiros.");
        }
        if (financialAmount != null && financialAmount.signum() < 0) {
            throw new InvalidRequestException("O valor financeiro não pode ser negativo.");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("A página deve ser não negativa e o tamanho deve estar entre 1 e 100.");
        }
    }

    private String clean(String value) {
        return value.trim();
    }

    private String cleanNullable(String value) {
        return value == null ? null : value.trim();
    }
}
