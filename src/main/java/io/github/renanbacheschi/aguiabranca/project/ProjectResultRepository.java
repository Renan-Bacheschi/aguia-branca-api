package io.github.renanbacheschi.aguiabranca.project;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectResultRepository extends MongoRepository<ProjectResultDocument, String> {
    List<ProjectResultDocument> findAllByProjectId(String projectId);

    List<ProjectResultDocument> findAllByProjectIdIn(Collection<String> projectIds);
}
