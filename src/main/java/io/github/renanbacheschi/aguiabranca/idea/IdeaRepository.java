package io.github.renanbacheschi.aguiabranca.idea;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdeaRepository extends MongoRepository<IdeaDocument, String> {
}
