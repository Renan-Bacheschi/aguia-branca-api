package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import io.github.renanbacheschi.aguiabranca.common.PageResponse;
import io.github.renanbacheschi.aguiabranca.error.ConflictException;
import io.github.renanbacheschi.aguiabranca.error.InvalidRequestException;
import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;
import io.github.renanbacheschi.aguiabranca.idea.IdeaDocument;
import io.github.renanbacheschi.aguiabranca.idea.IdeaService;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final IdeaService ideaService;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public ProjectService(
            ProjectRepository projectRepository,
            IdeaService ideaService,
            MongoTemplate mongoTemplate,
            Clock clock) {
        this.projectRepository = projectRepository;
        this.ideaService = ideaService;
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public ProjectResponse create(ProjectCreateRequest request, String userId) {
        validatePeriod(request.plannedStartDate(), request.plannedEndDate());
        IdeaDocument idea = ideaService.requireApproved(request.ideaId());
        if (projectRepository.existsByIdeaId(request.ideaId())) {
            throw new ConflictException("Já existe um projeto para esta ideia.");
        }
        ProjectDocument project = ProjectDocument.create(
                request.ideaId(), idea.getStrategyId(), clean(request.name()), clean(request.description()),
                clean(request.responsibleName()), request.plannedStartDate(), request.plannedEndDate(),
                request.plannedInvestment(), userId, clock.instant());
        try {
            return ProjectResponse.from(projectRepository.save(project));
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("Já existe um projeto para esta ideia.");
        }
    }

    public PageResponse<ProjectResponse> list(
            String strategyId,
            String ideaId,
            ProjectStatus status,
            ProjectStage stage,
            int page,
            int size) {
        validatePage(page, size);
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("archived").is(false));
        if (strategyId != null && !strategyId.isBlank()) criteria.add(Criteria.where("strategyId").is(strategyId));
        if (ideaId != null && !ideaId.isBlank()) criteria.add(Criteria.where("ideaId").is(ideaId));
        if (status != null) criteria.add(Criteria.where("status").is(status));
        if (stage != null) criteria.add(Criteria.where("stage").is(stage));

        Query query = new Query(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ProjectDocument.class);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        query.with(pageable);
        List<ProjectDocument> content = mongoTemplate.find(query, ProjectDocument.class);
        return PageResponse.from(new PageImpl<>(content, pageable, total), ProjectResponse::from);
    }

    public ProjectResponse get(String id) {
        return ProjectResponse.from(requireProject(id));
    }

    public ProjectResponse update(String id, ProjectUpdateRequest request) {
        validatePeriod(request.plannedStartDate(), request.plannedEndDate());
        ProjectDocument project = requireMutable(id);
        if (project.getActualEndDate() != null && project.getActualEndDate().isBefore(request.plannedStartDate())) {
            throw new InvalidRequestException("A data final realizada não pode ser anterior à data inicial planejada.");
        }
        project.update(
                clean(request.name()), clean(request.description()), clean(request.responsibleName()),
                request.plannedStartDate(), request.plannedEndDate(), request.plannedInvestment(), clock.instant());
        return ProjectResponse.from(projectRepository.save(project));
    }

    public void archive(String id) {
        ProjectDocument project = requireProject(id);
        if (project.isArchived()) {
            throw new ConflictException("O projeto já está arquivado.");
        }
        project.archive(clock.instant());
        projectRepository.save(project);
    }

    public ProjectResponse updateProgress(String id, ProgressUpdateRequest request, String userId) {
        ProjectDocument project = requireMutable(id);
        if (project.getStatus() == ProjectStatus.COMPLETED || project.getStatus() == ProjectStatus.CANCELLED) {
            throw new ConflictException("Projetos concluídos ou cancelados não aceitam novas atualizações de progresso.");
        }
        validateProgress(project, request);
        BigDecimal actualInvestment = request.actualInvestment() == null
                ? project.getActualInvestment()
                : request.actualInvestment();
        project.updateProgress(
                request.stage(), request.status(), request.progressPercentage(), actualInvestment,
                request.actualEndDate(), userId, clock.instant());
        return ProjectResponse.from(projectRepository.save(project));
    }

    public ProjectDocument requireProject(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado."));
    }

    public ProjectDocument requireResultMutationAllowed(String id) {
        ProjectDocument project = requireProject(id);
        if (project.isArchived()) {
            throw new ConflictException("Projetos arquivados não aceitam resultados.");
        }
        if (project.getStatus() == ProjectStatus.CANCELLED) {
            throw new ConflictException("Projetos cancelados não aceitam resultados.");
        }
        return project;
    }

    private ProjectDocument requireMutable(String id) {
        ProjectDocument project = requireProject(id);
        if (project.isArchived()) {
            throw new ConflictException("Um projeto arquivado não pode ser alterado.");
        }
        return project;
    }

    private void validateProgress(ProjectDocument project, ProgressUpdateRequest request) {
        int progress = request.progressPercentage();
        if (progress < project.getProgressPercentage()) {
            throw new ConflictException("O percentual de progresso não pode diminuir.");
        }
        if (request.stage().ordinal() < project.getStage().ordinal()) {
            throw new ConflictException("A etapa do projeto não pode retroceder.");
        }

        boolean validCombination = switch (request.status()) {
            case PLANNED -> request.stage() == ProjectStage.PLANNING
                    && progress == 0 && request.actualEndDate() == null;
            case IN_PROGRESS -> (request.stage() == ProjectStage.EXECUTION
                    || request.stage() == ProjectStage.MONITORING)
                    && progress < 100 && request.actualEndDate() == null;
            case COMPLETED -> request.stage() == ProjectStage.CLOSED
                    && progress == 100 && request.actualEndDate() != null;
            case CANCELLED -> request.stage() == ProjectStage.CLOSED && progress < 100;
        };
        if (!validCombination) {
            throw new InvalidRequestException("Etapa, status, progresso e data final são incoerentes.");
        }
        if (request.actualEndDate() != null && request.actualEndDate().isBefore(project.getPlannedStartDate())) {
            throw new InvalidRequestException("A data final realizada não pode ser anterior à data inicial planejada.");
        }
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidRequestException("A data final planejada não pode ser anterior à data inicial planejada.");
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
}
