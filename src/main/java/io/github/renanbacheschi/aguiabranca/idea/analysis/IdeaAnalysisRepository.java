package io.github.renanbacheschi.aguiabranca.idea.analysis;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdeaAnalysisRepository extends MongoRepository<IdeaAnalysisDocument, String> {

    Optional<IdeaAnalysisDocument> findFirstByIdeaIdOrderByGeneratedAtDesc(String ideaId);
}
