package io.github.renanbacheschi.aguiabranca.project;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectRepository extends MongoRepository<ProjectDocument, String> {
    boolean existsByIdeaId(String ideaId);
}
