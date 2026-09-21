package io.github.renanbacheschi.aguiabranca.project;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectResultRepository extends MongoRepository<ProjectResultDocument, String> {
}
