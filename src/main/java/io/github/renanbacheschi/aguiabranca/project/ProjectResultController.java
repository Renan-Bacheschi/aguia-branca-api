package io.github.renanbacheschi.aguiabranca.project;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.renanbacheschi.aguiabranca.auth.AuthenticatedUserContext;
import io.github.renanbacheschi.aguiabranca.common.PageResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/results")
public class ProjectResultController {

    private final ProjectResultService resultService;

    public ProjectResultController(ProjectResultService resultService) {
        this.resultService = resultService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    ResponseEntity<ProjectResultResponse> create(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectResultCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ProjectResultResponse response = resultService.create(
                projectId, request, AuthenticatedUserContext.from(jwt).userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','LEADER')")
    PageResponse<ProjectResultResponse> list(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return resultService.list(projectId, page, size);
    }

    @GetMapping("/{resultId}")
    @PreAuthorize("hasAnyRole('MANAGER','LEADER')")
    ProjectResultResponse get(@PathVariable String projectId, @PathVariable String resultId) {
        return resultService.get(projectId, resultId);
    }

    @PutMapping("/{resultId}")
    @PreAuthorize("hasRole('MANAGER')")
    ProjectResultResponse update(
            @PathVariable String projectId,
            @PathVariable String resultId,
            @Valid @RequestBody ProjectResultUpdateRequest request) {
        return resultService.update(projectId, resultId, request);
    }

    @DeleteMapping("/{resultId}")
    @PreAuthorize("hasRole('MANAGER')")
    ResponseEntity<Void> delete(@PathVariable String projectId, @PathVariable String resultId) {
        resultService.delete(projectId, resultId);
        return ResponseEntity.noContent().build();
    }
}
