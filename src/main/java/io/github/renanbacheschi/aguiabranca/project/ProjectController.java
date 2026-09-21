package io.github.renanbacheschi.aguiabranca.project;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ProjectResponse response = projectService.create(request, AuthenticatedUserContext.from(jwt).userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','LEADER')")
    PageResponse<ProjectResponse> list(
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String ideaId,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) ProjectStage stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return projectService.list(strategyId, ideaId, status, stage, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','LEADER')")
    ProjectResponse get(@PathVariable String id) {
        return projectService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    ProjectResponse update(@PathVariable String id, @Valid @RequestBody ProjectUpdateRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    ResponseEntity<Void> archive(@PathVariable String id) {
        projectService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasRole('MANAGER')")
    ProjectResponse updateProgress(
            @PathVariable String id,
            @Valid @RequestBody ProgressUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return projectService.updateProgress(id, request, AuthenticatedUserContext.from(jwt).userId());
    }
}
