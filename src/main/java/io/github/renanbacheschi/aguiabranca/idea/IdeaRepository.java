package io.github.renanbacheschi.aguiabranca.idea;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdeaRepository extends MongoRepository<IdeaDocument, String> {
    List<IdeaDocument> findAllByStrategyId(String strategyId);
}
