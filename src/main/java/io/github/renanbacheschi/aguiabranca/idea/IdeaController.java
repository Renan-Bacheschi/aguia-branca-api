package io.github.renanbacheschi.aguiabranca.idea;

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
@RequestMapping("/api/v1/ideas")
public class IdeaController {

    private final IdeaService ideaService;

    public IdeaController(IdeaService ideaService) {
        this.ideaService = ideaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    ResponseEntity<IdeaResponse> create(
            @Valid @RequestBody IdeaCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        IdeaResponse response = ideaService.create(request, AuthenticatedUserContext.from(jwt).userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR','MANAGER')")
    PageResponse<IdeaResponse> list(
            @RequestParam(required = false) IdeaStatus status,
            @RequestParam(required = false) IdeaPriority priority,
            @RequestParam(required = false) String strategyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return ideaService.list(
                status, priority, strategyId, page, size, AuthenticatedUserContext.from(jwt));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR','MANAGER')")
    IdeaResponse get(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ideaService.get(id, AuthenticatedUserContext.from(jwt));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    IdeaResponse update(
            @PathVariable String id,
            @Valid @RequestBody IdeaUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ideaService.update(id, request, AuthenticatedUserContext.from(jwt).userId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        ideaService.delete(id, AuthenticatedUserContext.from(jwt).userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('OPERATOR')")
    IdeaResponse submit(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ideaService.submit(id, AuthenticatedUserContext.from(jwt).userId());
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('MANAGER')")
    IdeaResponse review(
            @PathVariable String id,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ideaService.review(id, request, AuthenticatedUserContext.from(jwt).userId());
    }
}
