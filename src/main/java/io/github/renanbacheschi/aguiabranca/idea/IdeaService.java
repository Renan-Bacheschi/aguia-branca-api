package io.github.renanbacheschi.aguiabranca.idea;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import io.github.renanbacheschi.aguiabranca.auth.AuthenticatedUserContext;
import io.github.renanbacheschi.aguiabranca.common.PageResponse;
import io.github.renanbacheschi.aguiabranca.error.ConflictException;
import io.github.renanbacheschi.aguiabranca.error.InvalidRequestException;
import io.github.renanbacheschi.aguiabranca.error.ResourceNotFoundException;
import io.github.renanbacheschi.aguiabranca.strategy.StrategyService;
import io.github.renanbacheschi.aguiabranca.user.UserRole;

@Service
public class IdeaService {

    private final IdeaRepository ideaRepository;
    private final StrategyService strategyService;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public IdeaService(
            IdeaRepository ideaRepository,
            StrategyService strategyService,
            MongoTemplate mongoTemplate,
            Clock clock) {
        this.ideaRepository = ideaRepository;
        this.strategyService = strategyService;
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public IdeaResponse create(IdeaCreateRequest request, String userId) {
        strategyService.requireActive(request.strategyId());
        IdeaDocument idea = IdeaDocument.create(
                clean(request.title()), clean(request.problem()), clean(request.proposedSolution()),
                clean(request.expectedBenefits()), request.strategyId(), userId, clock.instant());
        return IdeaResponse.from(ideaRepository.save(idea));
    }

    public PageResponse<IdeaResponse> list(
            IdeaStatus status,
            IdeaPriority priority,
            String strategyId,
            int page,
            int size,
            AuthenticatedUserContext user) {
        validatePage(page, size);
        List<Criteria> criteria = new ArrayList<>();
        if (user.role() == UserRole.OPERATOR) {
            criteria.add(Criteria.where("authorUserId").is(user.userId()));
        } else if (user.role() == UserRole.MANAGER) {
            criteria.add(Criteria.where("status").ne(IdeaStatus.DRAFT));
        } else {
            throw new ResourceNotFoundException("Ideia não encontrada.");
        }
        if (status != null) criteria.add(Criteria.where("status").is(status));
        if (priority != null) criteria.add(Criteria.where("priority").is(priority));
        if (strategyId != null && !strategyId.isBlank()) criteria.add(Criteria.where("strategyId").is(strategyId));

        Query query = new Query();
        if (!criteria.isEmpty()) query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), IdeaDocument.class);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        query.with(pageable);
        List<IdeaDocument> content = mongoTemplate.find(query, IdeaDocument.class);
        return PageResponse.from(new PageImpl<>(content, pageable, total), IdeaResponse::from);
    }

    public IdeaResponse get(String id, AuthenticatedUserContext user) {
        return IdeaResponse.from(requireVisible(id, user));
    }

    public IdeaResponse update(String id, IdeaUpdateRequest request, String userId) {
        IdeaDocument idea = requireOwned(id, userId);
        requireDraft(idea);
        if (!idea.getStrategyId().equals(request.strategyId())) {
            strategyService.requireActive(request.strategyId());
        }
        idea.update(
                clean(request.title()), clean(request.problem()), clean(request.proposedSolution()),
                clean(request.expectedBenefits()), request.strategyId(), clock.instant());
        return IdeaResponse.from(ideaRepository.save(idea));
    }

    public void delete(String id, String userId) {
        IdeaDocument idea = requireOwned(id, userId);
        requireDraft(idea);
        ideaRepository.delete(idea);
    }

    public IdeaResponse submit(String id, String userId) {
        IdeaDocument idea = requireOwned(id, userId);
        if (idea.getStatus() == IdeaStatus.SUBMITTED) {
            return IdeaResponse.from(idea);
        }
        if (idea.getStatus() != IdeaStatus.DRAFT) {
            throw new ConflictException("A ideia não pode ser enviada no estado atual.");
        }
        strategyService.requireActive(idea.getStrategyId());
        idea.submit(clock.instant());
        return IdeaResponse.from(ideaRepository.save(idea));
    }

    public IdeaResponse review(String id, ReviewRequest request, String reviewerUserId) {
        if (request.decision() != IdeaStatus.APPROVED && request.decision() != IdeaStatus.REJECTED) {
            throw new InvalidRequestException("A decisão deve ser APPROVED ou REJECTED.");
        }
        IdeaDocument idea = ideaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ideia não encontrada."));
        if (idea.getStatus() != IdeaStatus.SUBMITTED) {
            throw new ConflictException("Somente ideias enviadas podem ser avaliadas.");
        }
        idea.review(
                request.decision(), request.priority(), clean(request.justification()),
                reviewerUserId, clock.instant());
        return IdeaResponse.from(ideaRepository.save(idea));
    }

    public IdeaDocument requireApproved(String id) {
        IdeaDocument idea = ideaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ideia não encontrada."));
        if (idea.getStatus() != IdeaStatus.APPROVED) {
            throw new ConflictException("O projeto deve ser criado a partir de uma ideia aprovada.");
        }
        return idea;
    }

    private IdeaDocument requireVisible(String id, AuthenticatedUserContext user) {
        IdeaDocument idea = ideaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ideia não encontrada."));
        if (user.role() == UserRole.OPERATOR && !idea.getAuthorUserId().equals(user.userId())) {
            throw new ResourceNotFoundException("Ideia não encontrada.");
        }
        if (user.role() == UserRole.MANAGER && idea.getStatus() == IdeaStatus.DRAFT) {
            throw new ResourceNotFoundException("Ideia não encontrada.");
        }
        return idea;
    }

    private IdeaDocument requireOwned(String id, String userId) {
        IdeaDocument idea = ideaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ideia não encontrada."));
        if (!idea.getAuthorUserId().equals(userId)) {
            throw new ResourceNotFoundException("Ideia não encontrada.");
        }
        return idea;
    }

    private void requireDraft(IdeaDocument idea) {
        if (idea.getStatus() != IdeaStatus.DRAFT) {
            throw new ConflictException("Somente ideias em rascunho podem ser alteradas ou removidas.");
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
